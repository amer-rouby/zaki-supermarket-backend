package com.zakisupermarket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zakisupermarket.dto.response.AlertStatsResponse;
import com.zakisupermarket.dto.response.StockAlertResponse;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.Product;
import com.zakisupermarket.entity.StockAlert;
import com.zakisupermarket.entity.StockBatch;
import com.zakisupermarket.entity.User;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.ProductRepository;
import com.zakisupermarket.repository.StockAlertRepository;
import com.zakisupermarket.repository.StockBatchRepository;
import com.zakisupermarket.service.StockAlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAlertServiceImpl implements StockAlertService {

    private final StockAlertRepository alertRepository;
    private final StockBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<StockAlertResponse> getAlerts(Long storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return alertRepository.findByStoreId(storeId, pageable)
                .map(StockAlertResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockAlertResponse> getAlertsByStatus(Long storeId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return alertRepository.findByStoreIdAndStatus(storeId, status, pageable)
                .map(StockAlertResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAlertResponse> getActiveAlerts(Long storeId) {
        return alertRepository.findActiveAlerts(storeId)
                .stream()
                .map(StockAlertResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AlertStatsResponse getAlertStats(Long storeId) {
        Long totalAlerts = alertRepository.countByStoreId(storeId);
        Long unreadAlerts = alertRepository.countUnreadAlerts(storeId);
        Long lowStockAlerts = alertRepository.countActiveAlertsByType(storeId, StockAlert.AlertType.LOW_STOCK);
        Long expiredAlerts = alertRepository.countActiveAlertsByType(storeId, StockAlert.AlertType.EXPIRED);
        Long expiringSoonAlerts = alertRepository.countActiveAlertsByType(storeId, StockAlert.AlertType.EXPIRING_SOON);
        Long outOfStockAlerts = alertRepository.countActiveAlertsByType(storeId, StockAlert.AlertType.OUT_OF_STOCK);

        return AlertStatsResponse.builder()
                .totalAlerts(totalAlerts)
                .unreadAlerts(unreadAlerts)
                .lowStockAlerts(lowStockAlerts)
                .expiredAlerts(expiredAlerts)
                .expiringSoonAlerts(expiringSoonAlerts)
                .outOfStockAlerts(outOfStockAlerts)
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(Long alertId, Long storeId, Long userId) {
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        if (!alert.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied");
        }
        alert.setStatus("READ");
        alert.setReadAt(LocalDateTime.now());
        alertRepository.save(alert);
        log.info("Alert {} marked as read by user {}", alertId, userId);
    }

    @Override
    @Transactional
    public void markAllAsRead(Long storeId, Long userId) {
        List<StockAlert> alerts = alertRepository.findByStoreIdAndStatusAndCreatedAtAfter(
                storeId, "UNREAD", LocalDateTime.now().minusDays(30)
        );
        alerts.forEach(alert -> {
            alert.setStatus("READ");
            alert.setReadAt(LocalDateTime.now());
        });
        alertRepository.saveAll(alerts);
        log.info("All alerts marked as read for store {} by user {}", storeId, userId);
    }

    @Override
    @Transactional
    public void resolveAlert(Long alertId, Long storeId, Long userId) {
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        if (!alert.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied");
        }
        alert.setStatus("RESOLVED");
        alert.setResolvedAt(LocalDateTime.now());
        alert.setResolvedBy(User.builder().id(userId).build());
        alertRepository.save(alert);
        log.info("Alert {} resolved by user {}", alertId, userId);
    }

    @Override
    @Transactional
    public void deleteAlert(Long alertId, Long storeId) {
        StockAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new RuntimeException("Alert not found"));
        if (!alert.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied");
        }
        alertRepository.delete(alert);
        log.info("Alert {} deleted for store {}", alertId, storeId);
    }

    @Override
    @Transactional
    public StockAlertResponse createAlert(Long storeId, Long productId, Long batchId,
                                          StockAlert.AlertType type, String title, String message, String severity) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        Product product = null;
        if (productId != null) {
            try {
                product = productRepository.findById(productId).orElse(null);
                if (product != null && product.getDeletedAt() != null) {
                    product = null;
                }
            } catch (Exception e) {
                log.warn("Could not load product {}: {}", productId, e.getMessage());
            }
        }

        StockBatch batch = null;
        if (batchId != null) {
            try {
                batch = batchRepository.findById(batchId).orElse(null);
            } catch (Exception e) {
                log.warn("Could not load batch {}: {}", batchId, e.getMessage());
            }
        }

        long recentCount = alertRepository.countRecentSimilarAlerts(
                storeId, type, productId, batchId, LocalDateTime.now().minusHours(24));

        if (recentCount > 0) {
            log.info("Similar alert already exists for type: {}, product: {}, batch: {}", type, productId, batchId);
            return null;
        }

        StockAlert alert = StockAlert.builder()
                .store(store)
                .product(product)
                .batch(batch)
                .alertType(type)
                .title(title)
                .message(message)
                .severity(severity != null ? severity : "MEDIUM")
                .status("UNREAD")
                .build();

        Map<String, Object> metadata = new HashMap<>();
        if (product != null) {
            metadata.put("productName", product.getName());
            if (product.getBarcode() != null) metadata.put("productCode", product.getBarcode());
        }
        if (batch != null) {
            metadata.put("batchNumber", batch.getBatchNumber());
            metadata.put("currentStock", batch.getQuantityCurrent());
            if (batch.getExpiryDate() != null) {
                metadata.put("expiryDate", batch.getExpiryDate().toString());
            }
        }

        try {
            alert.setMetadata(objectMapper.writeValueAsString(metadata));
        } catch (Exception e) {
            log.error("Error serializing metadata", e);
        }

        StockAlert saved = alertRepository.save(alert);
        log.info("Alert created: type={}, product={}, message={}", type, productId, title);
        return StockAlertResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void generateLowStockAlerts(Long storeId) {
        log.info("Generating low stock alerts for store: {}", storeId);
        List<Product> products = productRepository.findByStoreId(storeId);
        int createdCount = 0;

        // One bulk query for every product's stock total instead of one query per
        // product - this method runs on every /api/alerts* GET, so an N+1 loop here
        // meant a full-catalog query storm (439 products = 439 queries) on every call.
        Map<Long, Long> stockByProductId = new HashMap<>();
        for (Object[] row : batchRepository.sumQuantityGroupedByProductForStore(storeId)) {
            stockByProductId.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        }

        for (Product product : products) {
            if (product.getDeletedAt() != null) {
                continue;
            }

            Long totalStock = stockByProductId.getOrDefault(product.getId(), 0L);
            if (totalStock != null && totalStock <= product.getMinStockLevel()) {
                String title = totalStock == 0 ? "Out of Stock" : "Low Stock";
                String message = String.format("Product '%s' - Current stock: %d (Minimum: %d)",
                        product.getName(), totalStock, product.getMinStockLevel());
                StockAlert.AlertType type = totalStock == 0 ?
                        StockAlert.AlertType.OUT_OF_STOCK : StockAlert.AlertType.LOW_STOCK;

                StockAlertResponse alert = createAlert(storeId, product.getId(), null, type, title, message,
                        totalStock == 0 ? "CRITICAL" : "HIGH");
                if (alert != null) createdCount++;
            } else {
                alertRepository.autoResolveStockAlertsForProduct(storeId, product.getId(),
                        List.of(StockAlert.AlertType.LOW_STOCK, StockAlert.AlertType.OUT_OF_STOCK));
            }
        }
        log.info("Created {} low stock alerts", createdCount);
    }

    @Override
    @Transactional
    public void generateExpiryAlerts(Long storeId) {
        log.info("Generating expiry alerts for store: {}", storeId);
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysLater = today.plusDays(30);
        int expiredCount = 0, expiringCount = 0;

        List<StockBatch> expiredBatches = batchRepository.findExpiredBatches(storeId, today);
        for (StockBatch batch : expiredBatches) {
            if (batch.getStatus() != com.zakisupermarket.entity.StockBatch.BatchStatus.DISCARDED) {
                try {
                    Product product = batch.getProduct();
                    if (product == null) continue;

                    if (!Hibernate.isInitialized(product)) {
                        Hibernate.initialize(product);
                    }

                    if (product.getDeletedAt() != null) {
                        continue;
                    }

                    long daysSinceExpiry = java.time.temporal.ChronoUnit.DAYS.between(batch.getExpiryDate(), today);
                    String title = "Expired Product";
                    String message = String.format("Product '%s' (Batch %s) expired %d day(s) ago",
                            product.getName(), batch.getBatchNumber(), daysSinceExpiry);

                    StockAlertResponse alert = createAlert(storeId, product.getId(), batch.getId(),
                            StockAlert.AlertType.EXPIRED, title, message, "CRITICAL");
                    if (alert != null) expiredCount++;
                } catch (Exception e) {
                    log.warn("Skipping expired batch {} due to error: {}", batch.getId(), e.getMessage());
                }
            }
        }

        List<StockBatch> expiringBatches = batchRepository.findExpiringBatches(storeId, thirtyDaysLater);
        for (StockBatch batch : expiringBatches) {
            // findExpiringBatches has no lower bound, so it also returns already-expired
            // batches (handled above) - skip those here to avoid a second, contradictory
            // "expiring in -N days" alert for the same batch.
            if (batch.getExpiryDate().isBefore(today)) {
                continue;
            }
            if (batch.getStatus() == com.zakisupermarket.entity.StockBatch.BatchStatus.ACTIVE) {
                try {
                    Product product = batch.getProduct();
                    if (product == null) continue;

                    if (!Hibernate.isInitialized(product)) {
                        Hibernate.initialize(product);
                    }

                    if (product.getDeletedAt() != null) {
                        continue;
                    }

                    long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(today, batch.getExpiryDate());
                    String title = "Expiring Soon";
                    String message = String.format("Product '%s' (Batch %s) expires in %d day(s)",
                            product.getName(), batch.getBatchNumber(), daysUntilExpiry);
                    String severity = daysUntilExpiry <= 7 ? "HIGH" : "MEDIUM";

                    StockAlertResponse alert = createAlert(storeId, product.getId(), batch.getId(),
                            StockAlert.AlertType.EXPIRING_SOON, title, message, severity);
                    if (alert != null) expiringCount++;
                } catch (Exception e) {
                    log.warn("Skipping expiring batch {} due to error: {}", batch.getId(), e.getMessage());
                }
            }
        }
        log.info("Created {} expired alerts, {} expiring soon alerts", expiredCount, expiringCount);
    }
}