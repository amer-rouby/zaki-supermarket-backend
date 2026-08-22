package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.response.AnomalyDetectionResponse;
import com.zakisupermarket.entity.AnomalyDetection;
import com.zakisupermarket.entity.PurchaseOrder;
import com.zakisupermarket.entity.SaleTransaction;
import com.zakisupermarket.entity.StockAdjustmentHistory;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.User;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.repository.AnomalyDetectionRepository;
import com.zakisupermarket.repository.PurchaseOrderRepository;
import com.zakisupermarket.repository.SaleTransactionRepository;
import com.zakisupermarket.repository.StockAdjustmentHistoryRepository;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.UserRepository;
import com.zakisupermarket.service.AnomalyDetectionService;
import com.zakisupermarket.service.settings.ZakiFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private final AnomalyDetectionRepository anomalyRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final SaleTransactionRepository saleTransactionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final StockAdjustmentHistoryRepository stockAdjustmentHistoryRepository;
    private final ZakiFeatureSettingsService zakiFeatureSettingsService;

    private static final int WINDOW_HOURS = 24;
    private static final int EXCESSIVE_RETURNS_THRESHOLD = 3;
    private static final int EXCESSIVE_RETURNS_HIGH_THRESHOLD = 6;
    private static final int FREQUENT_CANCELLATIONS_THRESHOLD = 3;
    private static final int FREQUENT_CANCELLATIONS_HIGH_THRESHOLD = 6;
    private static final int REPEATED_ADJUSTMENTS_THRESHOLD = 5;
    private static final int REPEATED_ADJUSTMENTS_HIGH_THRESHOLD = 10;
    private static final int LARGE_DISCREPANCY_MIN_DELTA = 50;
    private static final int LARGE_DISCREPANCY_HIGH_DELTA = 100;
    private static final int MIN_HISTORY_FOR_DISCOUNT_BASELINE = 5;
    private static final double DISCOUNT_STD_DEV_THRESHOLD = 2.0;

    @Override
    @Scheduled(cron = "0 30 * * * *")
    @Transactional
    public void runDetectionForAllStores() {
        log.info("Running scheduled anomaly detection...");
        for (Store store : storeRepository.findByDeletedAtIsNull()) {
            try {
                runDetectionForStore(store.getId());
            } catch (Exception e) {
                log.error("Error running anomaly detection for store {}: {}", store.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void runDetectionForStore(Long storeId) {
        Boolean enabled = zakiFeatureSettingsService.getOrCreate(storeId).getAnomalyDetectionEnabled();
        if (enabled != null && !enabled) {
            return;
        }

        Store store = storeRepository.findById(storeId).orElse(null);
        if (store == null) return;

        LocalDateTime since = LocalDateTime.now().minusHours(WINDOW_HOURS);

        checkExcessiveReturns(store, since);
        checkFrequentCancellations(store, since);
        checkRepeatedStockAdjustments(store, since);
        checkLargeStockDiscrepancies(store, since);
        checkUnusualDiscounts(store, since);
    }

    private void checkExcessiveReturns(Store store, LocalDateTime since) {
        List<Object[]> rows = saleTransactionRepository.countDeletedSalesByUserSince(store.getId(), since);
        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            String userName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            if (count < EXCESSIVE_RETURNS_THRESHOLD) continue;

            if (isDuplicate(store.getId(), AnomalyDetection.Type.EXCESSIVE_RETURNS, "USER", userId, since)) continue;

            AnomalyDetection.Severity severity = count >= EXCESSIVE_RETURNS_HIGH_THRESHOLD
                    ? AnomalyDetection.Severity.HIGH : AnomalyDetection.Severity.MEDIUM;

            create(store, AnomalyDetection.Type.EXCESSIVE_RETURNS, severity,
                    "Unusual activity detected: " + userName + " had " + count
                            + " sale(s) cancelled/returned in the last 24 hours, more than usual.",
                    "USER", userId, userName);
        }
    }

    private void checkFrequentCancellations(Store store, LocalDateTime since) {
        List<Object[]> rows = purchaseOrderRepository.countCancelledOrdersByUserSince(store.getId(), since);
        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            String userName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            if (count < FREQUENT_CANCELLATIONS_THRESHOLD) continue;

            if (isDuplicate(store.getId(), AnomalyDetection.Type.FREQUENT_CANCELLATIONS, "USER", userId, since)) continue;

            AnomalyDetection.Severity severity = count >= FREQUENT_CANCELLATIONS_HIGH_THRESHOLD
                    ? AnomalyDetection.Severity.HIGH : AnomalyDetection.Severity.MEDIUM;

            create(store, AnomalyDetection.Type.FREQUENT_CANCELLATIONS, severity,
                    "Unusual activity detected: " + userName + " cancelled " + count
                            + " purchase order(s) in the last 24 hours, more than usual.",
                    "USER", userId, userName);
        }
    }

    private void checkRepeatedStockAdjustments(Store store, LocalDateTime since) {
        List<Object[]> rows = stockAdjustmentHistoryRepository.countAdjustmentsByUserSince(store.getId(), since);
        for (Object[] row : rows) {
            Long userId = ((Number) row[0]).longValue();
            String userName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            if (count < REPEATED_ADJUSTMENTS_THRESHOLD) continue;

            if (isDuplicate(store.getId(), AnomalyDetection.Type.REPEATED_STOCK_ADJUSTMENTS, "USER", userId, since)) continue;

            AnomalyDetection.Severity severity = count >= REPEATED_ADJUSTMENTS_HIGH_THRESHOLD
                    ? AnomalyDetection.Severity.HIGH : AnomalyDetection.Severity.MEDIUM;

            create(store, AnomalyDetection.Type.REPEATED_STOCK_ADJUSTMENTS, severity,
                    "Unusual activity detected: " + userName + " made " + count
                            + " stock adjustment(s) in the last 24 hours, more than usual.",
                    "USER", userId, userName);
        }
    }

    private void checkLargeStockDiscrepancies(Store store, LocalDateTime since) {
        List<StockAdjustmentHistory> rows = stockAdjustmentHistoryRepository
                .findLargeDiscrepanciesSince(store.getId(), since, LARGE_DISCREPANCY_MIN_DELTA);

        for (StockAdjustmentHistory h : rows) {
            if (isDuplicate(store.getId(), AnomalyDetection.Type.LARGE_STOCK_DISCREPANCY, "STOCK_ADJUSTMENT", h.getId(), null)) continue;

            int delta = Math.abs(h.getNewQuantity() - h.getPreviousQuantity());
            AnomalyDetection.Severity severity = delta >= LARGE_DISCREPANCY_HIGH_DELTA
                    ? AnomalyDetection.Severity.HIGH : AnomalyDetection.Severity.MEDIUM;

            String batchNumber = h.getBatch() != null ? h.getBatch().getBatchNumber() : "?";
            create(store, AnomalyDetection.Type.LARGE_STOCK_DISCREPANCY, severity,
                    "Unusual activity detected: stock adjustment for batch " + batchNumber
                            + " changed quantity from " + h.getPreviousQuantity() + " to " + h.getNewQuantity()
                            + " (" + delta + " unit(s)), larger than usual.",
                    "STOCK_ADJUSTMENT", h.getId(), batchNumber);
        }
    }

    private void checkUnusualDiscounts(Store store, LocalDateTime since) {
        List<SaleTransaction> recentSales = saleTransactionRepository
                .findByStoreIdAndTransactionDateBetween(store.getId(), since, LocalDateTime.now());

        for (SaleTransaction sale : recentSales) {
            if (sale.getUser() == null || sale.getSubtotal() == null
                    || sale.getSubtotal().compareTo(BigDecimal.ZERO) <= 0) continue;

            double saleDiscountPct = discountPercent(sale);
            if (saleDiscountPct <= 0) continue;

            List<SaleTransaction> history = saleTransactionRepository.findByStoreIdAndUserIdAndTransactionDateAfter(
                    store.getId(), sale.getUser().getId(), LocalDateTime.now().minusDays(30));
            history.removeIf(s -> s.getId().equals(sale.getId()));

            if (history.size() < MIN_HISTORY_FOR_DISCOUNT_BASELINE) continue;

            double[] pcts = history.stream()
                    .filter(s -> s.getSubtotal() != null && s.getSubtotal().compareTo(BigDecimal.ZERO) > 0)
                    .mapToDouble(this::discountPercent)
                    .toArray();
            if (pcts.length < MIN_HISTORY_FOR_DISCOUNT_BASELINE) continue;

            double mean = 0;
            for (double p : pcts) mean += p;
            mean /= pcts.length;

            double variance = 0;
            for (double p : pcts) variance += Math.pow(p - mean, 2);
            variance /= pcts.length;
            double stdDev = Math.sqrt(variance);

            double threshold = mean + DISCOUNT_STD_DEV_THRESHOLD * stdDev;
            if (stdDev <= 0 || saleDiscountPct <= threshold) continue;

            if (isDuplicate(store.getId(), AnomalyDetection.Type.UNUSUAL_DISCOUNT, "SALE", sale.getId(), null)) continue;

            String userName = sale.getUser().getFullName();
            create(store, AnomalyDetection.Type.UNUSUAL_DISCOUNT, AnomalyDetection.Severity.MEDIUM,
                    "Unusual activity detected: a " + round1(saleDiscountPct) + "% discount on sale "
                            + (sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : ("#" + sale.getId()))
                            + " by " + userName + " is well above their usual average of " + round1(mean) + "%.",
                    "SALE", sale.getId(), sale.getInvoiceNumber());
        }
    }

    private double discountPercent(SaleTransaction sale) {
        if (sale.getSubtotal() == null || sale.getSubtotal().compareTo(BigDecimal.ZERO) <= 0
                || sale.getDiscountAmount() == null) return 0;
        return sale.getDiscountAmount()
                .divide(sale.getSubtotal(), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

    private double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private boolean isDuplicate(Long storeId, AnomalyDetection.Type type, String relatedEntityType,
                                  Long relatedEntityId, LocalDateTime since) {
        LocalDateTime effectiveSince = since != null ? since : LocalDateTime.now().minusYears(10);
        return !anomalyRepository.findRecentDuplicates(storeId, type, relatedEntityType, relatedEntityId, effectiveSince).isEmpty();
    }

    private void create(Store store, AnomalyDetection.Type type, AnomalyDetection.Severity severity,
                          String description, String relatedEntityType, Long relatedEntityId, String relatedEntityName) {
        AnomalyDetection anomaly = AnomalyDetection.builder()
                .store(store)
                .type(type)
                .severity(severity)
                .status(AnomalyDetection.Status.NEW)
                .description(description)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .relatedEntityName(relatedEntityName)
                .build();
        anomalyRepository.save(anomaly);
        log.info("Anomaly detected: store={}, type={}, severity={}", store.getId(), type, severity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AnomalyDetectionResponse> getAnomalies(Long storeId, AnomalyDetection.Status status,
                                                          AnomalyDetection.Type type, int page, int size) {
        Boolean enabled = zakiFeatureSettingsService.getOrCreate(storeId).getAnomalyDetectionEnabled();
        if (enabled != null && !enabled) {
            throw new FeatureDisabledException("FEATURE_DISABLED_ANOMALY_DETECTION", "Anomaly detection feature is disabled for this store");
        }
        PageRequest pageable = PageRequest.of(page, size);
        Page<AnomalyDetection> result;
        if (status != null && type != null) {
            result = anomalyRepository.findByStoreIdAndStatusAndTypeOrderByDetectedAtDesc(storeId, status, type, pageable);
        } else if (status != null) {
            result = anomalyRepository.findByStoreIdAndStatusOrderByDetectedAtDesc(storeId, status, pageable);
        } else if (type != null) {
            result = anomalyRepository.findByStoreIdAndTypeOrderByDetectedAtDesc(storeId, type, pageable);
        } else {
            result = anomalyRepository.findByStoreIdOrderByDetectedAtDesc(storeId, pageable);
        }
        return result.map(AnomalyDetectionResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(Long storeId, AnomalyDetection.Status status) {
        return anomalyRepository.countByStoreIdAndStatus(storeId, status);
    }

    @Override
    @Transactional
    public AnomalyDetectionResponse markReviewed(Long id, Long storeId, Long userId) {
        return updateStatus(id, storeId, userId, AnomalyDetection.Status.REVIEWED);
    }

    @Override
    @Transactional
    public AnomalyDetectionResponse dismiss(Long id, Long storeId, Long userId) {
        return updateStatus(id, storeId, userId, AnomalyDetection.Status.DISMISSED);
    }

    private AnomalyDetectionResponse updateStatus(Long id, Long storeId, Long userId, AnomalyDetection.Status status) {
        AnomalyDetection anomaly = anomalyRepository.findByIdAndStoreId(id, storeId)
                .orElseThrow(() -> new RuntimeException("Anomaly not found: " + id));
        anomaly.setStatus(status);
        anomaly.setReviewedAt(LocalDateTime.now());
        if (userId != null) {
            User user = userRepository.findById(userId).orElse(null);
            anomaly.setReviewedBy(user);
        }
        AnomalyDetection saved = anomalyRepository.save(anomaly);
        return AnomalyDetectionResponse.fromEntity(saved);
    }
}
