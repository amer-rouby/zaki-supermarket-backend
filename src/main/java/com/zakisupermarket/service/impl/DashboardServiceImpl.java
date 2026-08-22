package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.response.DashboardResponse;
import com.zakisupermarket.dto.response.DemandPredictionResponse;
import com.zakisupermarket.dto.response.ZakiInsightsDTO;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.entity.Product;
import com.zakisupermarket.entity.SaleTransaction;
import com.zakisupermarket.entity.StockBatch;
import com.zakisupermarket.entity.settings.ZakiFeatureSettings;
import com.zakisupermarket.repository.ProductRepository;
import com.zakisupermarket.repository.SaleTransactionRepository;
import com.zakisupermarket.repository.StockBatchRepository;
import com.zakisupermarket.service.DashboardService;
import com.zakisupermarket.service.DemandPredictionService;
import com.zakisupermarket.service.PricingRecommendationService;
import com.zakisupermarket.service.settings.ZakiFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardServiceImpl implements DashboardService {

    private final SaleTransactionRepository saleTransactionRepository;
    private final ProductRepository productRepository;
    private final StockBatchRepository stockBatchRepository;
    private final ZakiFeatureSettingsService zakiFeatureSettingsService;
    private final DemandPredictionService demandPredictionService;
    private final PricingRecommendationService pricingRecommendationService;

    private static final int SALES_TREND_WINDOW_DAYS = 30;
    private static final Set<String> HIGH_RISK_LEVELS = Set.of("CRITICAL", "HIGH");

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboardStats(Long storeId) {
        log.info("Fetching dashboard stats for storeId: {}", storeId);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        BigDecimal todayRevenue = saleTransactionRepository
                .sumTotalAmountByStoreIdAndDateRange(storeId, startOfDay, endOfDay);

        Long todayOrders = saleTransactionRepository
                .countByStoreIdAndDateRange(storeId, startOfDay, endOfDay);

        BigDecimal todayAverageOrder = (todayOrders != null && todayOrders > 0 && todayRevenue != null)
                ? todayRevenue.divide(BigDecimal.valueOf(todayOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Long totalProducts = productRepository.countByStoreId(storeId);
        Long lowStockProducts = productRepository.countLowStockProducts(storeId);
        Long outOfStockProducts = productRepository.countOutOfStockProducts(storeId);
        BigDecimal inventoryValue = calculateInventoryValue(storeId);

        LocalDate expiryThreshold = LocalDate.now().plusDays(30);
        Long expiringBatches = stockBatchRepository.countExpiringBatches(storeId, expiryThreshold);
        Long expiredBatches = stockBatchRepository.countExpiredBatches(storeId);
        
        List<DashboardResponse.TopProductDTO> topProducts = getTopProducts(storeId, 5);

        List<DashboardResponse.RecentSaleDTO> recentSales = getRecentSales(storeId, 5);

        return DashboardResponse.builder()
                .todayRevenue(todayRevenue != null ? todayRevenue : BigDecimal.ZERO)
                .todayOrders(todayOrders != null ? todayOrders : 0L)
                .todayAverageOrder(todayAverageOrder)
                .totalProducts(totalProducts != null ? totalProducts : 0L)
                .lowStockProducts(lowStockProducts != null ? lowStockProducts : 0L)
                .outOfStockProducts(outOfStockProducts != null ? outOfStockProducts : 0L)
                .inventoryValue(inventoryValue != null ? inventoryValue : BigDecimal.ZERO)
                .expiringBatches(expiringBatches != null ? expiringBatches : 0L)
                .expiredBatches(expiredBatches != null ? expiredBatches : 0L)
                .topProducts(topProducts)
                .recentSales(recentSales)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ZakiInsightsDTO getZakiInsights(Long storeId) {
        ZakiFeatureSettings flags = zakiFeatureSettingsService.getOrCreate(storeId);
        if (!isEnabled(flags.getDashboardInsightsEnabled())) {
            throw new FeatureDisabledException("FEATURE_DISABLED_DASHBOARD_INSIGHTS", "Dashboard insights feature is disabled for this store");
        }

        LocalDate today = LocalDate.now();
        BigDecimal todayRevenue = saleTransactionRepository.sumTotalAmountByStoreIdAndDateRange(
                storeId, today.atStartOfDay(), today.atTime(LocalTime.MAX));
        if (todayRevenue == null) todayRevenue = BigDecimal.ZERO;

        LocalDate trendStart = today.minusDays(SALES_TREND_WINDOW_DAYS);
        LocalDate trendEnd = today.minusDays(1);
        BigDecimal trendRevenue = saleTransactionRepository.sumTotalAmountByStoreIdAndDateRange(
                storeId, trendStart.atStartOfDay(), trendEnd.atTime(LocalTime.MAX));
        BigDecimal averageDailyRevenue = trendRevenue != null
                ? trendRevenue.divide(BigDecimal.valueOf(SALES_TREND_WINDOW_DAYS), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Double salesDeltaPercent = null;
        if (averageDailyRevenue.compareTo(BigDecimal.ZERO) > 0) {
            salesDeltaPercent = todayRevenue.subtract(averageDailyRevenue)
                    .divide(averageDailyRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .doubleValue();
        }

        Integer highRiskStockoutCount = null;
        if (isEnabled(flags.getStockPredictionEnabled())) {
            List<DemandPredictionResponse> upcoming = demandPredictionService.getUpcomingPredictions(storeId, 1);
            highRiskStockoutCount = (int) upcoming.stream()
                    .filter(p -> HIGH_RISK_LEVELS.contains(p.getRiskLevel()))
                    .count();
        }

        Integer reorderRecommendationsCount = null;
        if (isEnabled(flags.getReorderRecommendationsEnabled())) {
            reorderRecommendationsCount = demandPredictionService.getReorderRecommendations(storeId).size();
        }

        Integer pricingRecommendationsCount = null;
        if (isEnabled(flags.getPricingRecommendationsEnabled())) {
            pricingRecommendationsCount = pricingRecommendationService.getRecommendations(storeId).size();
        }

        return ZakiInsightsDTO.builder()
                .todayRevenue(todayRevenue)
                .averageDailyRevenue30d(averageDailyRevenue)
                .salesDeltaPercent(salesDeltaPercent)
                .highRiskStockoutCount(highRiskStockoutCount)
                .reorderRecommendationsCount(reorderRecommendationsCount)
                .pricingRecommendationsCount(pricingRecommendationsCount)
                .build();
    }

    private boolean isEnabled(Boolean flag) {
        return flag == null || flag;
    }

    private BigDecimal calculateInventoryValue(Long storeId) {
        List<Product> products = productRepository.findByStoreId(storeId);

        if (products == null || products.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return products.stream()
                .map(product -> {
                    BigDecimal totalStock = product.getStockBatches().stream()
                            .filter(batch -> batch != null)
                            .filter(batch -> batch.getStatus() == StockBatch.BatchStatus.ACTIVE)
                            .filter(batch -> batch.getQuantityCurrent() != null)
                            .map(batch -> BigDecimal.valueOf(batch.getQuantityCurrent()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    BigDecimal sellPrice = product.getSellPrice() != null
                            ? product.getSellPrice() : BigDecimal.ZERO;

                    return totalStock.multiply(sellPrice);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DashboardResponse.TopProductDTO> getTopProducts(Long storeId, int limit) {
        List<Object[]> results = saleTransactionRepository.findTopSellingProducts(
                storeId, PageRequest.of(0, limit));

        return results.stream()
                .map(obj -> DashboardResponse.TopProductDTO.builder()
                        .productId(obj[0] != null ? ((Number) obj[0]).longValue() : 0L)
                        .productName(obj[1] != null ? (String) obj[1] : "")
                        .quantitySold(obj[2] != null ? ((Number) obj[2]).longValue() : 0L)
                        .totalRevenue(obj[3] != null ? (BigDecimal) obj[3] : BigDecimal.ZERO)
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardResponse.RecentSaleDTO> getRecentSales(Long storeId, int limit) {
        List<SaleTransaction> sales = saleTransactionRepository.findRecentSalesByStoreId(
                storeId, PageRequest.of(0, limit));

        return sales.stream()
                .map(sale -> DashboardResponse.RecentSaleDTO.builder()
                        .saleId(sale.getId())
                        .invoiceNumber(sale.getInvoiceNumber() != null ? sale.getInvoiceNumber() : "")
                        .totalAmount(sale.getTotalAmount() != null ? sale.getTotalAmount() : BigDecimal.ZERO)
                        .transactionDate(sale.getTransactionDate() != null ? sale.getTransactionDate().toString() : "")
                        .paymentMethod(sale.getPaymentMethod() != null ? sale.getPaymentMethod().name() : "CASH")
                        .build())
                .collect(Collectors.toList());
    }
}