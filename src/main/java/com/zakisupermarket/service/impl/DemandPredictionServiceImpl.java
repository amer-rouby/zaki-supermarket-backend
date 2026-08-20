package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.UpdatePredictionDTO;
import com.zakisupermarket.dto.response.DemandPredictionResponse;
import com.zakisupermarket.dto.request.CreateShareLinkRequest;
import com.zakisupermarket.dto.response.ReorderRecommendationDTO;
import com.zakisupermarket.dto.response.SalesHistoryPointDTO;
import com.zakisupermarket.dto.response.SupplierReorderGroupDTO;
import com.zakisupermarket.dto.response.ShareLinkResponse;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.entity.DemandPrediction;
import com.zakisupermarket.entity.PurchaseOrderItem;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.Product;
import com.zakisupermarket.entity.SaleItem;
import com.zakisupermarket.entity.Supplier;
import com.zakisupermarket.repository.DemandPredictionRepository;
import com.zakisupermarket.repository.PurchaseOrderItemRepository;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.ProductRepository;
import com.zakisupermarket.repository.SaleItemRepository;
import com.zakisupermarket.service.DemandPredictionService;
import com.zakisupermarket.service.ShareLinkService;
import com.zakisupermarket.service.settings.ZakiFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DemandPredictionServiceImpl implements DemandPredictionService {

    private final DemandPredictionRepository predictionRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final SaleItemRepository saleItemRepository;
    private final ShareLinkService shareLinkService;
    private final ZakiFeatureSettingsService zakiFeatureSettingsService;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    private static final int MOVING_AVG_DAYS = 14;
    private static final int DEFAULT_PREDICTION = 10;
    private static final BigDecimal CONFIDENCE_BASE = BigDecimal.valueOf(0.75);
    private static final int FORECAST_HORIZON_DAYS = 7;
    private static final int RETENTION_DAYS = 60;
    private static final int REORDER_TARGET_DAYS_OF_STOCK = 14;

    @Override
    @Transactional
    public void generatePredictions(Long storeId, LocalDate forDate) {
        log.info("Starting prediction generation for store: {}, date: {}", storeId, forDate);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found with ID: " + storeId));
        List<Product> products = productRepository.findByStoreId(storeId);
        int success = 0;
        for (Product product : products) {
            try {
                generatePredictionForProduct(product.getId(), storeId, forDate);
                success++;
            } catch (Exception e) {
                log.error("Failed for product ID {}: {}", product.getId(), e.getMessage(), e);
            }
        }
        log.info("Completed. Success: {}, Failed: {}", success, products.size() - success);
    }

    @Override
    @Transactional
    public void generateWeeklyPredictionsForAllStores() {
        LocalDate cutoff = LocalDate.now().minusDays(RETENTION_DAYS);
        List<Store> stores = storeRepository.findAll();
        for (Store store : stores) {
            predictionRepository.deleteByStoreIdAndPredictionDateBefore(store.getId(), cutoff);
            for (int i = 1; i <= FORECAST_HORIZON_DAYS; i++) {
                generatePredictions(store.getId(), LocalDate.now().plusDays(i));
            }
        }
        log.info("Weekly prediction generation done for {} stores (retention cutoff: {})",
                stores.size(), cutoff);
    }

    @Override
    @Transactional
    public void updatePastPredictionsWithActuals() {
        LocalDate today = LocalDate.now();
        List<DemandPrediction> pastPredictions = predictionRepository
                .findByPredictionDateBeforeAndActualQuantityIsNull(today);
        int updated = 0;
        for (DemandPrediction prediction : pastPredictions) {
            try {
                Long productId = prediction.getProduct() != null ? prediction.getProduct().getId() : null;
                Long storeId = prediction.getStore() != null ? prediction.getStore().getId() : null;
                if (productId == null || storeId == null) continue;
                int actualQuantity = getActualSalesForDate(productId, storeId, prediction.getPredictionDate());
                prediction.setActualQuantity(actualQuantity);
                if (prediction.getPredictedQuantity() != null && prediction.getPredictedQuantity() > 0) {
                    int error = Math.abs(actualQuantity - prediction.getPredictedQuantity());
                    BigDecimal accuracy = BigDecimal.valueOf(
                            Math.max(0, 100.0 - (error * 100.0 / prediction.getPredictedQuantity())));
                    prediction.setAccuracyPercentage(accuracy);
                }
                predictionRepository.save(prediction);
                updated++;
            } catch (Exception e) {
                log.error("Failed to update actuals for prediction {}: {}", prediction.getId(), e.getMessage(), e);
            }
        }
        log.info("Updated {} of {} past predictions with actual sales data", updated, pastPredictions.size());
    }

    private int getActualSalesForDate(Long productId, Long storeId, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<SaleItem> items = saleItemRepository.findByProductIdAndStoreIdAndDateBetween(
                productId, storeId, start, end);
        if (items == null) return 0;
        return items.stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
    }

    @Override
    @Transactional
    public DemandPredictionResponse generatePredictionForProduct(Long productId, Long storeId, LocalDate forDate) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found: " + storeId));
        List<Integer> historicalSales = getHistoricalSales(productId, storeId, 30);
        Integer predictedQuantity = calculateSimpleForecast(historicalSales, forDate, product.getCategory());
        BigDecimal confidence = calculateConfidence(historicalSales, predictedQuantity);
        Integer currentStock = getCurrentStock(productId);
        DemandPrediction prediction = DemandPrediction.builder()
                .product(product)
                .store(store)
                .predictionDate(forDate)
                .predictedQuantity(predictedQuantity)
                .confidenceLevel(confidence)
                .algorithmVersion("v1-simple-moving-average")
                .factorsApplied(buildFactorsJson(forDate, product.getCategory()))
                .build();
        Optional<DemandPrediction> existing = predictionRepository
                .findByProductIdAndStoreIdAndPredictionDate(productId, storeId, forDate);
        if (existing.isPresent()) {
            prediction.setId(existing.get().getId());
        }
        DemandPrediction saved = predictionRepository.save(prediction);
        log.info("Prediction saved: product={}, date={}, predicted={}", productId, forDate, predictedQuantity);
        return DemandPredictionResponse.fromEntity(saved, currentStock, isStockPredictionEnabled(storeId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DemandPredictionResponse> getUpcomingPredictions(Long storeId, int daysAhead) {
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusDays(daysAhead);
        List<DemandPrediction> predictions = predictionRepository
                .findUpcomingPredictions(storeId, today, endDate);
        log.info("Found {} upcoming predictions for store {}", predictions.size(), storeId);
        boolean riskEnabled = isStockPredictionEnabled(storeId);
        return predictions.stream()
                .map(p -> {
                    Long prodId = p.getProduct() != null ? p.getProduct().getId() : null;
                    return DemandPredictionResponse.fromEntity(p, getCurrentStock(prodId), riskEnabled);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DemandPredictionResponse> getPredictions(Long storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        boolean riskEnabled = isStockPredictionEnabled(storeId);
        return predictionRepository
                .findByStoreIdAndPredictionDateGreaterThanEqualOrderByPredictionDateAsc(
                        storeId, LocalDate.now(), pageable)
                .map(p -> {
                    Long prodId = p.getProduct() != null ? p.getProduct().getId() : null;
                    return DemandPredictionResponse.fromEntity(p, getCurrentStock(prodId), riskEnabled);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public DemandPredictionResponse getPredictionById(Long predictionId, Long storeId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found: " + predictionId));
        if (!prediction.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied: Prediction does not belong to this store");
        }
        Long prodId = prediction.getProduct() != null ? prediction.getProduct().getId() : null;
        return DemandPredictionResponse.fromEntity(prediction, getCurrentStock(prodId), isStockPredictionEnabled(storeId));
    }

    @Override
    @Transactional
    public void updatePredictionWithActual(Long predictionId, Integer actualQuantity, Long storeId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found: " + predictionId));
        if (!prediction.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Prediction not found: " + predictionId);
        }
        prediction.setActualQuantity(actualQuantity);
        if (prediction.getPredictedQuantity() != null && prediction.getPredictedQuantity() > 0) {
            int error = Math.abs(actualQuantity - prediction.getPredictedQuantity());
            BigDecimal accuracy = BigDecimal.valueOf(Math.max(0, 100.0 - (error * 100.0 / prediction.getPredictedQuantity())));
            prediction.setAccuracyPercentage(accuracy);
        }
        predictionRepository.save(prediction);
        log.info("Prediction {} updated with actual: {}, accuracy: {}%",
                predictionId, actualQuantity, prediction.getAccuracyPercentage());
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getAccuracyStats(Long storeId) {
        Long totalPredictions = predictionRepository.countByStoreId(storeId);
        BigDecimal averageAccuracy = predictionRepository.calculateAverageAccuracyByStore(storeId);
        String lastUpdated = predictionRepository.findLatestPredictionDateByStore(storeId)
                .map(LocalDate::toString)
                .orElse(LocalDate.now().toString());
        return Map.of(
                "totalPredictions", totalPredictions != null ? totalPredictions : 0,
                "averageAccuracy", averageAccuracy != null ? averageAccuracy.doubleValue() : 0.0,
                "lastUpdated", lastUpdated
        );
    }

    @Override
    public Integer calculateSimpleForecast(List<Integer> historicalSales, LocalDate predictionDate, String productCategory) {
        if (historicalSales == null || historicalSales.isEmpty()) {
            return DEFAULT_PREDICTION;
        }
        int movingAvg = calculateMovingAverage(historicalSales, MOVING_AVG_DAYS);
        BigDecimal seasonalityFactor = getSeasonalityMultiplier(predictionDate, productCategory);
        BigDecimal adjusted = BigDecimal.valueOf(movingAvg).multiply(seasonalityFactor);
        BigDecimal trendFactor = calculateTrendFactor(historicalSales);
        adjusted = adjusted.multiply(trendFactor);
        if (predictionDate != null &&
                (predictionDate.getDayOfWeek() == DayOfWeek.FRIDAY || predictionDate.getDayOfWeek() == DayOfWeek.SATURDAY)) {
            adjusted = adjusted.multiply(BigDecimal.valueOf(1.15));
        }
        return Math.max(1, adjusted.intValue());
    }

    @Override
    @Transactional
    public DemandPredictionResponse updatePrediction(Long predictionId, UpdatePredictionDTO updates, Long storeId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found: " + predictionId));
        if (!prediction.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied: Prediction does not belong to this store");
        }
        if (updates.getPredictedQuantity() != null && updates.getPredictedQuantity() > 0) {
            prediction.setPredictedQuantity(updates.getPredictedQuantity());
        }
        if (updates.getConfidenceLevel() != null) {
            BigDecimal confidence = updates.getConfidenceLevel();
            if (confidence.compareTo(BigDecimal.ZERO) >= 0 && confidence.compareTo(BigDecimal.ONE) <= 0) {
                prediction.setConfidenceLevel(confidence);
            }
        }
        if (updates.getRecommendation() != null && !updates.getRecommendation().isEmpty()) {
            prediction.setFactorsApplied(updates.getRecommendation());
        }
        prediction.setUpdatedAt(LocalDateTime.now());
        DemandPrediction updated = predictionRepository.save(prediction);
        log.info("Prediction {} updated by store {}", predictionId, storeId);
        Integer currentStock = getCurrentStock(updated.getProduct() != null ? updated.getProduct().getId() : null);
        return DemandPredictionResponse.fromEntity(updated, currentStock);
    }

    @Override
    @Transactional
    public void deletePrediction(Long predictionId, Long storeId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found: " + predictionId));
        if (!prediction.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied: Prediction does not belong to this store");
        }
        predictionRepository.delete(prediction);
        log.info("Prediction {} deleted by store {}", predictionId, storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPredictionToPdf(Long predictionId, Long storeId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found"));
        if (!prediction.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied");
        }
        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        pdf.append("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");
        pdf.append("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n");
        pdf.append("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] >>\nendobj\n");
        pdf.append("xref\n0 4\n0000000000 65535 f \n");
        pdf.append("trailer\n<< /Size 4 /Root 1 0 R >>\nstartxref\n10\n%%EOF");
        return pdf.toString().getBytes();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPredictionToExcel(Long predictionId, Long storeId) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found"));
        if (!prediction.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied");
        }
        StringBuilder excel = new StringBuilder();
        excel.append("Prediction ID,Product Name,Predicted Quantity,Current Stock,Recommended Order,Confidence\n");
        excel.append(prediction.getId()).append(",");
        excel.append(prediction.getProduct() != null ? prediction.getProduct().getName() : "N/A").append(",");
        excel.append(prediction.getPredictedQuantity()).append(",");
        excel.append(getCurrentStock(prediction.getProduct() != null ? prediction.getProduct().getId() : null)).append(",");
        excel.append(Math.max(0, prediction.getPredictedQuantity() - getCurrentStock(prediction.getProduct() != null ? prediction.getProduct().getId() : null))).append(",");
        excel.append(prediction.getConfidenceLevel() != null ? prediction.getConfidenceLevel() : "N/A");
        return excel.toString().getBytes();
    }

    @Override
    @Transactional
    public ShareLinkResponse generateShareLink(Long predictionId, Long storeId, Long userId, int expiryHours) {
        DemandPrediction prediction = predictionRepository.findById(predictionId)
                .orElseThrow(() -> new RuntimeException("Prediction not found"));
        if (!prediction.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Access denied");
        }

        CreateShareLinkRequest request = CreateShareLinkRequest.builder()
                .entityType("prediction")
                .entityId(predictionId)
                .expiryHours(expiryHours)
                .build();

        return shareLinkService.createShareLink(request, userId, storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SalesHistoryPointDTO> getProductSalesHistory(Long productId, Long storeId, int days) {
        if (!isStockPredictionEnabled(storeId)) {
            throw new FeatureDisabledException("Stock prediction feature is disabled for this store");
        }
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<SaleItem> saleItems = saleItemRepository.findByProductIdAndStoreIdAndDateBetween(
                productId, storeId, startDateTime, endDateTime);

        Map<LocalDate, Integer> dailyQuantity = new HashMap<>();
        Map<LocalDate, BigDecimal> dailySales = new HashMap<>();
        if (saleItems != null) {
            for (SaleItem item : saleItems) {
                if (item.getTransaction() == null || item.getTransaction().getTransactionDate() == null) continue;
                LocalDate date = item.getTransaction().getTransactionDate().toLocalDate();
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                BigDecimal amount = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                dailyQuantity.merge(date, qty, Integer::sum);
                dailySales.merge(date, amount, BigDecimal::add);
            }
        }

        List<SalesHistoryPointDTO> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            result.add(SalesHistoryPointDTO.builder()
                    .date(date)
                    .quantity(dailyQuantity.getOrDefault(date, 0))
                    .sales(dailySales.getOrDefault(date, BigDecimal.ZERO))
                    .build());
        }
        return result;
    }

    private boolean isStockPredictionEnabled(Long storeId) {
        Boolean enabled = zakiFeatureSettingsService.getOrCreate(storeId).getStockPredictionEnabled();
        return enabled == null || enabled;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReorderRecommendationDTO> getReorderRecommendations(Long storeId) {
        Boolean enabled = zakiFeatureSettingsService.getOrCreate(storeId).getReorderRecommendationsEnabled();
        if (enabled != null && !enabled) {
            throw new FeatureDisabledException("Reorder recommendations feature is disabled for this store");
        }
        return computeReorderRecommendations(storeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SupplierReorderGroupDTO> getReorderRecommendationsBySupplier(Long storeId) {
        Boolean enabled = zakiFeatureSettingsService.getOrCreate(storeId).getSupplierRecommendationsEnabled();
        if (enabled != null && !enabled) {
            throw new FeatureDisabledException("Supplier order recommendations feature is disabled for this store");
        }

        List<ReorderRecommendationDTO> recommendations = computeReorderRecommendations(storeId);
        Map<Long, SupplierReorderGroupDTO> groups = new LinkedHashMap<>();

        for (ReorderRecommendationDTO rec : recommendations) {
            Long key = rec.getSupplierId() != null ? rec.getSupplierId() : -1L;
            SupplierReorderGroupDTO group = groups.computeIfAbsent(key, k -> SupplierReorderGroupDTO.builder()
                    .supplierId(rec.getSupplierId())
                    .supplierName(rec.getSupplierName())
                    .recommendations(new ArrayList<>())
                    .build());
            group.getRecommendations().add(rec);
        }

        List<SupplierReorderGroupDTO> result = new ArrayList<>(groups.values());
        result.sort(Comparator.comparing((SupplierReorderGroupDTO g) -> g.getSupplierId() == null)
                .thenComparing(g -> g.getSupplierName() != null ? g.getSupplierName() : ""));
        return result;
    }

    private List<ReorderRecommendationDTO> computeReorderRecommendations(Long storeId) {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<DemandPrediction> predictions = predictionRepository.findUpcomingPredictions(storeId, tomorrow, tomorrow);

        List<ReorderRecommendationDTO> result = new ArrayList<>();
        for (DemandPrediction p : predictions) {
            Long productId = p.getProduct() != null ? p.getProduct().getId() : null;
            Integer currentStock = getCurrentStock(productId);
            DemandPredictionResponse enriched = DemandPredictionResponse.fromEntity(p, currentStock, true);

            // The base recommendedOrder field only compares stock against a single
            // day's forecast, so a genuinely critical product can show 0 there.
            // Reordering needs a real coverage target instead.
            int dailyForecast = p.getPredictedQuantity() != null ? p.getPredictedQuantity() : 0;
            int recommendedQuantity = Math.max(0, dailyForecast * REORDER_TARGET_DAYS_OF_STOCK - currentStock);
            if (recommendedQuantity <= 0) continue;

            Supplier supplier = findInferredSupplier(productId, storeId);

            result.add(ReorderRecommendationDTO.builder()
                    .predictionId(p.getId())
                    .productId(productId)
                    .productName(enriched.getProductName())
                    .productCode(enriched.getProductCode())
                    .currentStock(currentStock)
                    .recommendedQuantity(recommendedQuantity)
                    .supplierId(supplier != null ? supplier.getId() : null)
                    .supplierName(supplier != null ? supplier.getName() : null)
                    .reason(buildReorderReason(enriched))
                    .priority(mapRiskToPriority(enriched.getRiskLevel()))
                    .riskLevel(enriched.getRiskLevel())
                    .daysUntilStockout(enriched.getDaysUntilStockout())
                    .build());
        }

        result.sort(Comparator
                .comparingInt((ReorderRecommendationDTO r) -> riskRank(r.getRiskLevel()))
                .thenComparing(Comparator.comparingInt(ReorderRecommendationDTO::getRecommendedQuantity).reversed()));

        return result;
    }

    private Supplier findInferredSupplier(Long productId, Long storeId) {
        if (productId == null) return null;
        List<PurchaseOrderItem> recent = purchaseOrderItemRepository
                .findMostRecentByProductIdAndStoreId(productId, storeId, PageRequest.of(0, 1));
        if (recent.isEmpty()) return null;
        return recent.get(0).getPurchaseOrder().getSupplier();
    }

    private String buildReorderReason(DemandPredictionResponse r) {
        if (r.getDaysUntilStockout() != null) {
            return "Stock expected to run out in " + r.getDaysUntilStockout() + " day(s) based on recent sales";
        }
        return "Recommended based on demand forecast";
    }

    private String mapRiskToPriority(String riskLevel) {
        if (riskLevel == null) return "LOW";
        return switch (riskLevel) {
            case "CRITICAL", "HIGH" -> "HIGH";
            case "MEDIUM" -> "MEDIUM";
            default -> "LOW";
        };
    }

    private int riskRank(String riskLevel) {
        if (riskLevel == null) return 4;
        return switch (riskLevel) {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            default -> 3;
        };
    }

    private int calculateMovingAverage(List<Integer> sales, int days) {
        if (sales == null || sales.isEmpty()) return 0;
        int sum = 0;
        int count = Math.min(sales.size(), days);
        for (int i = sales.size() - 1; i >= Math.max(0, sales.size() - days); i--) {
            sum += sales.get(i);
        }
        return count > 0 ? sum / count : 0;
    }

    private BigDecimal getSeasonalityMultiplier(LocalDate date, String category) {
        if (date == null) return BigDecimal.ONE;
        int month = date.getMonthValue();
        Map<String, Map<Integer, BigDecimal>> seasonalityMap = Map.of(
                "Beverages", Map.of(6, BigDecimal.valueOf(1.3), 7, BigDecimal.valueOf(1.4), 8, BigDecimal.valueOf(1.3)),
                "Household", Map.of(3, BigDecimal.valueOf(1.2), 4, BigDecimal.valueOf(1.3)),
                "Bakery", Map.of(9, BigDecimal.valueOf(1.2), 10, BigDecimal.valueOf(1.2)),
                "Dairy", Map.of(12, BigDecimal.valueOf(1.2), 1, BigDecimal.valueOf(1.2))
        );
        Map<Integer, BigDecimal> monthFactors = seasonalityMap.getOrDefault(category, Map.of());
        return monthFactors.getOrDefault(month, BigDecimal.ONE);
    }

    private BigDecimal calculateTrendFactor(List<Integer> sales) {
        if (sales == null || sales.size() < 7) return BigDecimal.ONE;
        int recentAvg = calculateMovingAverage(sales, 7);
        List<Integer> olderSales = sales.subList(0, Math.min(sales.size(), 14));
        int olderAvg = calculateMovingAverage(olderSales, 7);
        if (olderAvg == 0) return BigDecimal.ONE;
        BigDecimal ratio = BigDecimal.valueOf(recentAvg)
                .divide(BigDecimal.valueOf(olderAvg), 2, BigDecimal.ROUND_HALF_UP);
        return ratio.max(BigDecimal.valueOf(0.8)).min(BigDecimal.valueOf(1.3));
    }

    private BigDecimal calculateConfidence(List<Integer> historicalSales, Integer predicted) {
        if (historicalSales == null || historicalSales.size() < 7) {
            return CONFIDENCE_BASE.multiply(BigDecimal.valueOf(0.8));
        }
        BigDecimal dataConfidence = BigDecimal.valueOf(Math.min(1.0, historicalSales.size() / 30.0));
        double variance = calculateVariance(historicalSales);
        BigDecimal varianceConfidence = BigDecimal.valueOf(Math.max(0.5, 1.0 - (variance / 100.0)));
        return CONFIDENCE_BASE.multiply(dataConfidence)
                .multiply(varianceConfidence)
                .setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private double calculateVariance(List<Integer> values) {
        if (values == null || values.isEmpty()) return 0;
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0);
        return values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
    }

    private String buildFactorsJson(LocalDate date, String category) {
        Map<String, Object> factors = new HashMap<>();
        factors.put("seasonality", getSeasonalityMultiplier(date, category).doubleValue());
        factors.put("trend", "calculated");
        factors.put("dayOfWeek", date != null ? date.getDayOfWeek().name() : "UNKNOWN");
        factors.put("movingAvgDays", MOVING_AVG_DAYS);
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(factors);
        } catch (Exception e) {
            return "{}";
        }
    }

    private List<Integer> getHistoricalSales(Long productId, Long storeId, int days) {
        if (productId == null || storeId == null) return Collections.emptyList();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        List<SaleItem> saleItems = saleItemRepository.findByProductIdAndStoreIdAndDateBetween(
                productId, storeId, startDateTime, endDateTime);
        if (saleItems == null || saleItems.isEmpty()) return Collections.emptyList();
        Map<LocalDate, Integer> dailySales = saleItems.stream()
                .filter(item -> item.getTransaction() != null && item.getTransaction().getTransactionDate() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getTransaction().getTransactionDate().toLocalDate(),
                        Collectors.summingInt(item -> item.getQuantity() != null ? item.getQuantity() : 0)
                ));
        List<Integer> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            result.add(dailySales.getOrDefault(date, 0));
        }
        return result;
    }

    private Integer getCurrentStock(Long productId) {
        if (productId == null) return 0;
        Product product = productRepository.findById(productId).orElse(null);
        return product != null && product.getTotalStock() != null ? product.getTotalStock() : 0;
    }
}