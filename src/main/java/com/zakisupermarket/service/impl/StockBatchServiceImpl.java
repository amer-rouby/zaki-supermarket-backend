package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.StockAdjustmentRequest;
import com.zakisupermarket.dto.request.StockBatchRequest;
import com.zakisupermarket.dto.response.StockAdjustmentHistoryDTO;
import com.zakisupermarket.dto.response.StockBatchResponse;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.Product;
import com.zakisupermarket.entity.StockAdjustmentHistory;
import com.zakisupermarket.entity.StockBatch;
import com.zakisupermarket.entity.User;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.ProductRepository;
import com.zakisupermarket.repository.StockAdjustmentHistoryRepository;
import com.zakisupermarket.repository.StockBatchRepository;
import com.zakisupermarket.repository.UserRepository;
import com.zakisupermarket.service.NotificationStreamService;
import com.zakisupermarket.service.StockBatchService;
import com.zakisupermarket.service.settings.ZakiFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockBatchServiceImpl implements StockBatchService {

    private final StockBatchRepository stockBatchRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final StockAdjustmentHistoryRepository stockAdjustmentHistoryRepository;
    private final NotificationStreamService notificationStreamService;
    private final ZakiFeatureSettingsService zakiFeatureSettingsService;

    @Override
    @Transactional(readOnly = true)
    public Page<StockBatchResponse> getAllBatches(Long storeId, int page, int size) {
        log.debug("Fetching batches for store: {}, page: {}, size: {}", storeId, page, size);
        Pageable pageable = PageRequest.of(page, size);
        return stockBatchRepository.findByStoreIdAndStatus(storeId, StockBatch.BatchStatus.ACTIVE, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public StockBatchResponse getBatch(Long id, Long storeId) {
        log.debug("Fetching batch {} for store: {}", id, storeId);
        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + id));

        if (!Hibernate.isInitialized(batch.getStore())) {
            Hibernate.initialize(batch.getStore());
        }
        if (!batch.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied: Batch does not belong to this store");
        }
        return mapToResponse(batch);
    }

    @Override
    @Transactional
    public StockBatchResponse createBatch(StockBatchRequest request, Long storeId, Long userId) {
        log.info("Creating new batch for store: {}, user: {}, product: {}",
                storeId, userId, request.getProductId());

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User ID is required");
        }

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found with id: " + storeId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + request.getProductId()));

        if (!product.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Product does not belong to this store");
        }

        BigDecimal buyPrice = request.getBuyPrice();
        if (buyPrice == null) {
            buyPrice = product.getBuyPrice();
            if (buyPrice == null) {
                buyPrice = BigDecimal.ZERO;
            }
        }

        BigDecimal sellPrice = request.getSellPrice();
        if (sellPrice == null) {
            sellPrice = product.getSellPrice();
            if (sellPrice == null) {
                sellPrice = buyPrice.multiply(BigDecimal.valueOf(1.25)).setScale(2, BigDecimal.ROUND_HALF_UP);
            }
        }

        User userRef = null;
        if (userId != null) {
            try {
                userRef = userRepository.getReferenceById(userId);
            } catch (Exception e) {
                log.warn("Could not load user reference for userId: {}", userId);
            }
        }

        StockBatch batch = StockBatch.builder()
                .product(product)
                .store(store)
                .batchNumber(request.getBatchNumber())
                .quantityCurrent(request.getQuantityInitial())
                .quantityInitial(request.getQuantityInitial())
                .expiryDate(request.getExpiryDate())
                .productionDate(request.getProductionDate())
                .buyPrice(buyPrice)
                .sellPrice(sellPrice)
                .location(request.getLocation())
                .shelf(request.getShelf())
                .warehouse(request.getWarehouse())
                .notes(request.getNotes())
                .createdBy(userRef)
                .status(StockBatch.BatchStatus.ACTIVE)
                .build();

        StockBatch saved = stockBatchRepository.save(batch);
        log.info("Batch created successfully: id={}, batchNumber={}", saved.getId(), saved.getBatchNumber());
        StockBatchResponse response = mapToResponse(saved);
        notifyRealtimeStockChange(storeId, response, "CREATED");
        return response;
    }

    @Override
    @Transactional
    public StockBatchResponse updateBatch(Long id, StockBatchRequest request, Long storeId, Long userId) {
        log.info("Updating batch {} for store: {}, user: {}", id, storeId, userId);

        if (userId == null) {
            log.warn("User ID is null for batch update: {}. Proceeding without user tracking.", id);
        }

        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + id));

        if (!batch.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied: Batch does not belong to this store");
        }

        batch.setBatchNumber(request.getBatchNumber());
        batch.setQuantityInitial(request.getQuantityInitial());
        batch.setQuantityCurrent(request.getQuantityCurrent());
        batch.setExpiryDate(request.getExpiryDate());
        batch.setProductionDate(request.getProductionDate());

        if (request.getStatus() != null) {
            batch.setStatus(StockBatch.BatchStatus.valueOf(request.getStatus()));
        }

        if (request.getBuyPrice() != null) {
            batch.setBuyPrice(request.getBuyPrice());
        }
        if (request.getSellPrice() != null) {
            batch.setSellPrice(request.getSellPrice());
        }

        batch.setLocation(request.getLocation());
        batch.setShelf(request.getShelf());
        batch.setWarehouse(request.getWarehouse());
        batch.setNotes(request.getNotes());
        batch.setUpdatedAt(java.time.LocalDateTime.now());

        StockBatch updated = stockBatchRepository.save(batch);
        log.info("Batch updated successfully: id={}, status={}", updated.getId(), updated.getStatus());
        StockBatchResponse response = mapToResponse(updated);
        notifyRealtimeStockChange(storeId, response, "UPDATED");
        return response;
    }

    @Override
    @Transactional
    public void deleteBatch(Long id, Long storeId, Long userId) {
        log.info("Deleting batch {} for store: {}, user: {}", id, storeId, userId);

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User ID is required");
        }

        StockBatch batch = stockBatchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + id));

        if (!batch.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied: Batch does not belong to this store");
        }

        batch.setStatus(StockBatch.BatchStatus.DISCARDED);
        batch.setUpdatedAt(LocalDateTime.now());
        StockBatch discarded = stockBatchRepository.save(batch);
        log.info("Batch marked as discarded: id={}", id);
        notifyRealtimeStockChange(storeId, mapToResponse(discarded), "DELETED");
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchResponse> getExpiringBatches(Long storeId, int days) {
        log.debug("Fetching expiring batches for store: {}, days: {}", storeId, days);
        LocalDate thresholdDate = LocalDate.now().plusDays(days);
        return stockBatchRepository.findExpiringBatches(storeId, thresholdDate)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockBatchResponse> getExpiredBatches(Long storeId) {
        log.debug("Fetching expired batches for store: {}", storeId);
        LocalDate today = LocalDate.now();
        return stockBatchRepository.findExpiredBatches(storeId, today)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public StockBatchResponse adjustStock(Long batchId, StockAdjustmentRequest request, Long userId, Long storeId) {
        log.info("Adjusting stock for batch: {}, type: {}, quantity: {}, user: {}",
                batchId, request.getType(), request.getQuantity(), userId);

        if (userId == null) {
            throw new RuntimeException("Unauthorized: User ID is required for stock adjustment");
        }

        StockBatch batch = stockBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found with id: " + batchId));

        if (!batch.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Batch not found with id: " + batchId);
        }

        Integer currentQuantity = batch.getQuantityCurrent();
        Integer adjustmentQuantity = request.getQuantity();
        String type = request.getType();

        Integer newQuantity = switch (type) {
            case "ADD" -> currentQuantity + adjustmentQuantity;
            case "REMOVE" -> {
                if (adjustmentQuantity > currentQuantity) {
                    throw new RuntimeException("Insufficient stock: current=" + currentQuantity + ", requested=" + adjustmentQuantity);
                }
                yield currentQuantity - adjustmentQuantity;
            }
            case "CORRECTION" -> adjustmentQuantity;
            default -> throw new IllegalArgumentException("Invalid adjustment type: " + type);
        };

        batch.setQuantityCurrent(newQuantity);
        updateBatchStatus(batch, newQuantity);

        StockAdjustmentHistory history = StockAdjustmentHistory.builder()
                .batch(batch)
                .type(type)
                .quantity(adjustmentQuantity)
                .reason(request.getReason())
                .previousQuantity(currentQuantity)
                .newQuantity(newQuantity)
                .notes(request.getNotes())
                .adjustedBy(userId)
                .build();

        stockAdjustmentHistoryRepository.save(history);

        String shortNote = String.format("[%s] %s: %d→%d",
                request.getReason(), type, currentQuantity, newQuantity);
        batch.setNotes(shortNote);

        batch.setUpdatedAt(LocalDateTime.now());

        StockBatch updated = stockBatchRepository.save(batch);
        log.info("Stock adjusted | batchId: {} | type: {} | qty: {} | {}→{} | status: {}",
                batchId, type, adjustmentQuantity, currentQuantity, newQuantity, updated.getStatus());

        StockBatchResponse response = mapToResponse(updated);
        notifyRealtimeStockChange(storeId, response, "ADJUSTED");
        return response;
    }

    private void notifyRealtimeStockChange(Long storeId, StockBatchResponse batch, String changeType) {
        try {
            Boolean enabled = zakiFeatureSettingsService.getOrCreate(storeId).getRealtimeUpdatesEnabled();
            if (enabled != null && !enabled) return;
            notificationStreamService.notifyStockChanged(storeId, Map.of("changeType", changeType, "batch", batch));
        } catch (Exception e) {
            log.warn("Failed to send real-time stock update for store {}: {}", storeId, e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentHistoryDTO> getAdjustmentHistory(Long batchId, Long storeId) {
        StockBatch batch = stockBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Batch not found"));

        if (!batch.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied");
        }

        return stockAdjustmentHistoryRepository.findByBatchIdOrderByAdjustmentDateDesc(batchId)
                .stream()
                .map(this::mapHistoryToDTO)
                .collect(Collectors.toList());
    }

    private void updateBatchStatus(StockBatch batch, Integer newQuantity) {
        if (batch.isExpired()) {
            batch.setStatus(StockBatch.BatchStatus.EXPIRED);
        } else if (newQuantity <= 0) {
            batch.setStatus(StockBatch.BatchStatus.EXPIRED);
        } else if (newQuantity < batch.getProduct().getMinStockLevel()) {
            batch.setStatus(StockBatch.BatchStatus.LOW);
        } else {
            batch.setStatus(StockBatch.BatchStatus.ACTIVE);
        }
    }

    private StockBatchResponse mapToResponse(StockBatch batch) {
        try {
            if (batch == null) {
                return null;
            }

            String productName = "Product unavailable";
            String productBarcode = null;
            Long productId = null;

            try {
                if (batch.getProduct() != null) {
                    if (!Hibernate.isInitialized(batch.getProduct())) {
                        try {
                            Hibernate.initialize(batch.getProduct());
                        } catch (Exception e) {
                            log.warn("Could not initialize product for batch {}: {}",
                                    batch.getId(), e.getMessage());
                        }
                    }

                    if (batch.getProduct().getDeletedAt() == null) {
                        productId = batch.getProduct().getId();
                        productName = batch.getProduct().getName() != null ?
                                batch.getProduct().getName() : "Product unavailable";
                        productBarcode = batch.getProduct().getBarcode();
                    }
                }
            } catch (Exception e) {
                log.warn("Error loading product for batch {}: {}", batch.getId(), e.getMessage());
            }

            return StockBatchResponse.builder()
                    .id(batch.getId())
                    .productId(productId)
                    .productName(productName)
                    .productBarcode(productBarcode)
                    .storeId(batch.getStore().getId())
                    .batchNumber(batch.getBatchNumber())
                    .quantityCurrent(batch.getQuantityCurrent())
                    .quantityInitial(batch.getQuantityInitial())
                    .expiryDate(batch.getExpiryDate())
                    .productionDate(batch.getProductionDate())
                    .buyPrice(batch.getBuyPrice())
                    .sellPrice(batch.getSellPrice())
                    .location(batch.getLocation())
                    .shelf(batch.getShelf())
                    .warehouse(batch.getWarehouse())
                    .status(batch.getStatus() != null ? batch.getStatus().name() : null)
                    .notes(batch.getNotes())
                    .createdAt(batch.getCreatedAt())
                    .updatedAt(batch.getUpdatedAt())
                    .isExpired(batch.isExpired())
                    .isExpiringSoon(batch.isExpiringSoon(30))
                    .build();

        } catch (Exception e) {
            log.error("Failed to map batch {} to response: {}",
                    batch != null ? batch.getId() : "unknown", e.getMessage());
            return StockBatchResponse.builder()
                    .id(batch != null ? batch.getId() : null)
                    .productName("Error loading data")
                    .build();
        }
    }

    private StockAdjustmentHistoryDTO mapHistoryToDTO(StockAdjustmentHistory history) {
        try {
            String productName = "Product unavailable";
            try {
                if (history.getBatch() != null && history.getBatch().getProduct() != null) {
                    if (!Hibernate.isInitialized(history.getBatch().getProduct())) {
                        Hibernate.initialize(history.getBatch().getProduct());
                    }
                    if (history.getBatch().getProduct().getDeletedAt() == null) {
                        productName = history.getBatch().getProduct().getName() != null ?
                                history.getBatch().getProduct().getName() : "Product unavailable";
                    }
                }
            } catch (Exception e) {
                log.warn("Could not load product for history {}: {}", history.getId(), e.getMessage());
            }

            return StockAdjustmentHistoryDTO.builder()
                    .id(history.getId())
                    .batchId(history.getBatch() != null ? history.getBatch().getId() : null)
                    .batchNumber(history.getBatch() != null ? history.getBatch().getBatchNumber() : null)
                    .productName(productName)
                    .type(history.getType())
                    .quantity(history.getQuantity())
                    .reason(history.getReason())
                    .previousQuantity(history.getPreviousQuantity())
                    .newQuantity(history.getNewQuantity())
                    .notes(history.getNotes())
                    .adjustmentDate(history.getAdjustmentDate())
                    .adjustedBy(history.getAdjustedBy())
                    .adjustedByName(history.getAdjustedByName())
                    .build();
        } catch (Exception e) {
            log.error("Failed to map history {} to DTO", history != null ? history.getId() : "unknown", e);
            return StockAdjustmentHistoryDTO.builder()
                    .id(history != null ? history.getId() : null)
                    .productName("Error loading data")
                    .build();
        }
    }
}