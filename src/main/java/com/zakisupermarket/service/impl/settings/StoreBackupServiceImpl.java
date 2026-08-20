package com.zakisupermarket.service.impl.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakisupermarket.dto.settings.request.BackupRequest;
import com.zakisupermarket.dto.settings.response.BackupResponse;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.settings.BackupRecord;
import com.zakisupermarket.repository.*;
import com.zakisupermarket.repository.settings.BackupRecordRepository;
import com.zakisupermarket.service.settings.StoreBackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreBackupServiceImpl implements StoreBackupService {

    private static final int MAX_ROWS_PER_TABLE = 200_000;

    private final BackupRecordRepository backupRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final StockBatchRepository stockBatchRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final SaleItemRepository saleItemRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final ExpenseRepository expenseRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${backup.directory:./backups}")
    private String backupDirectory;

    @Override
    @Transactional
    public BackupResponse createBackup(Long storeId, Long userId, BackupRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportedAt", LocalDateTime.now().toString());
        export.put("storeId", storeId);
        export.put("storeName", store.getName());
        export.put("products", productRepository.findByStoreId(storeId));
        export.put("categories", categoryRepository.findByStoreId(storeId));
        export.put("suppliers", supplierRepository.findByStoreId(storeId));
        export.put("stockBatches", stockBatchRepository.findByStoreId(storeId));
        export.put("saleTransactions",
                saleTransactionRepository.findByStoreId(storeId, PageRequest.of(0, MAX_ROWS_PER_TABLE)).getContent());
        export.put("saleItems", saleItemRepository.findByStoreId(storeId));
        export.put("purchaseOrders",
                purchaseOrderRepository.findByStoreId(storeId, PageRequest.of(0, MAX_ROWS_PER_TABLE)).getContent());
        export.put("purchaseOrderItems", purchaseOrderItemRepository.findByStoreId(storeId));
        export.put("expenses", expenseRepository.findByStoreIdAndDeletedAtIsNull(storeId));
        export.put("payments",
                paymentRepository.findByStoreId(storeId, PageRequest.of(0, MAX_ROWS_PER_TABLE)).getContent());
        export.put("users", userRepository.findByStoreId(storeId).stream()
                // Never include password hashes in an exportable file.
                .map(u -> {
                    Map<String, Object> safe = new LinkedHashMap<>();
                    safe.put("id", u.getId());
                    safe.put("username", u.getUsername());
                    safe.put("fullName", u.getFullName());
                    safe.put("email", u.getEmail());
                    safe.put("phone", u.getPhone());
                    safe.put("role", u.getRole());
                    safe.put("isActive", u.getIsActive());
                    return safe;
                })
                .collect(Collectors.toList()));

        try {
            Path backupPath = Paths.get(backupDirectory, "store");
            if (!Files.exists(backupPath)) {
                Files.createDirectories(backupPath);
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String fileName = "store_" + storeId + "_" + sanitize(request.getBackupName()) + "_" + timestamp + ".json";
            Path fullPath = backupPath.resolve(fileName);

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(fullPath.toFile(), export);
            long fileSize = Files.size(fullPath);

            BackupRecord record = BackupRecord.builder()
                    .backupName(request.getBackupName())
                    .filePath(fullPath.toString())
                    .fileSize(fileSize)
                    .backupType(request.getBackupType())
                    .status("COMPLETED")
                    .description(request.getDescription())
                    .createdBy(userId)
                    .storeId(storeId)
                    .scope("STORE")
                    .build();
            record = backupRepository.save(record);

            log.info("Store backup created: storeId={}, file={}, {} bytes", storeId, fileName, fileSize);
            return toResponse(record);

        } catch (IOException e) {
            log.error("Error creating store backup for storeId={}", storeId, e);
            throw new RuntimeException("Failed to create backup: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<BackupResponse> getBackupsForStore(Long storeId) {
        return backupRepository.findByStoreIdOrderByCreatedAtDesc(storeId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BackupResponse getBackup(Long id, Long storeId) {
        return toResponse(findOwned(id, storeId));
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadBackup(Long id, Long storeId) {
        BackupRecord record = findOwned(id, storeId);
        try {
            return Files.readAllBytes(Paths.get(record.getFilePath()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to download backup: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void deleteBackup(Long id, Long storeId) {
        BackupRecord record = findOwned(id, storeId);
        try {
            Files.deleteIfExists(Paths.get(record.getFilePath()));
        } catch (IOException e) {
            log.error("Error deleting store backup file", e);
        }
        backupRepository.delete(record);
        log.info("Store backup deleted: id={}, storeId={}", id, storeId);
    }

    /** Loads a backup by id and verifies it actually belongs to this store and is
     * a STORE-scope record - never lets a store admin reach a whole-database
     * PLATFORM backup or another store's export through this path. */
    private BackupRecord findOwned(Long id, Long storeId) {
        BackupRecord record = backupRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Backup not found"));
        if (!"STORE".equals(record.getScope()) || !storeId.equals(record.getStoreId())) {
            throw new RuntimeException("Backup not found");
        }
        return record;
    }

    private BackupResponse toResponse(BackupRecord record) {
        return BackupResponse.fromEntity(record);
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) {
            return "backup";
        }
        return name.replaceAll("[^a-zA-Z0-9_-]", "_");
    }
}
