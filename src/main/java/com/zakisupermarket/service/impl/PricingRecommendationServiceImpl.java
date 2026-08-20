package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.response.PricingRecommendationDTO;
import com.zakisupermarket.entity.Product;
import com.zakisupermarket.entity.StockBatch;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.repository.ProductRepository;
import com.zakisupermarket.repository.SaleItemRepository;
import com.zakisupermarket.repository.StockBatchRepository;
import com.zakisupermarket.service.PricingRecommendationService;
import com.zakisupermarket.service.settings.ZakiFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingRecommendationServiceImpl implements PricingRecommendationService {

    private final StockBatchRepository stockBatchRepository;
    private final ProductRepository productRepository;
    private final SaleItemRepository saleItemRepository;
    private final ZakiFeatureSettingsService zakiFeatureSettingsService;

    private static final int URGENT_EXPIRY_DAYS = 7;
    private static final int WARNING_EXPIRY_DAYS = 30;
    private static final int URGENT_DISCOUNT_PERCENT = 30;
    private static final int WARNING_DISCOUNT_PERCENT = 15;
    private static final int SLOW_MOVER_DISCOUNT_PERCENT = 10;
    private static final int VELOCITY_BASELINE_DAYS = 90;
    private static final int VELOCITY_RECENT_DAYS = 30;
    private static final double SLOW_MOVER_THRESHOLD_RATIO = 0.2;

    @Override
    @Transactional(readOnly = true)
    public List<PricingRecommendationDTO> getRecommendations(Long storeId) {
        Boolean enabled = zakiFeatureSettingsService.getOrCreate(storeId).getPricingRecommendationsEnabled();
        if (enabled != null && !enabled) {
            throw new FeatureDisabledException("Pricing recommendations feature is disabled for this store");
        }

        List<PricingRecommendationDTO> result = new ArrayList<>();
        result.addAll(buildExpiryRecommendations(storeId));
        result.addAll(buildSlowMoverRecommendations(storeId));

        result.sort(Comparator.comparingInt(r -> priorityRank(r.getPriority())));
        return result;
    }

    private List<PricingRecommendationDTO> buildExpiryRecommendations(Long storeId) {
        List<PricingRecommendationDTO> recommendations = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate threshold = today.plusDays(WARNING_EXPIRY_DAYS);
        List<StockBatch> batches = stockBatchRepository.findExpiringBatches(storeId, threshold);

        for (StockBatch batch : batches) {
            if (batch.getQuantityCurrent() == null || batch.getQuantityCurrent() <= 0) continue;
            if (batch.getProduct() == null) continue;

            long daysUntilExpiry = ChronoUnit.DAYS.between(today, batch.getExpiryDate());
            int discountPercent;
            String priority;
            String message;

            if (daysUntilExpiry < URGENT_EXPIRY_DAYS) {
                discountPercent = URGENT_DISCOUNT_PERCENT;
                priority = "HIGH";
                message = "Expires in " + daysUntilExpiry + " day(s) - clear stock now";
            } else {
                discountPercent = WARNING_DISCOUNT_PERCENT;
                priority = "MEDIUM";
                message = "Expires in " + daysUntilExpiry + " day(s) - consider discounting soon";
            }

            recommendations.add(PricingRecommendationDTO.builder()
                    .productId(batch.getProduct().getId())
                    .productName(batch.getProduct().getName())
                    .productCode(batch.getProduct().getCode())
                    .batchId(batch.getId())
                    .batchNumber(batch.getBatchNumber())
                    .expiryDate(batch.getExpiryDate())
                    .daysUntilExpiry((int) daysUntilExpiry)
                    .currentStock(batch.getQuantityCurrent())
                    .reason("EXPIRING")
                    .suggestedDiscountPercent(discountPercent)
                    .priority(priority)
                    .message(message)
                    .build());
        }
        return recommendations;
    }

    private List<PricingRecommendationDTO> buildSlowMoverRecommendations(Long storeId) {
        List<PricingRecommendationDTO> recommendations = new ArrayList<>();
        List<Product> products = productRepository.findByStoreId(storeId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime baselineStart = now.minusDays(VELOCITY_BASELINE_DAYS).with(LocalTime.MIN);
        LocalDateTime recentStart = now.minusDays(VELOCITY_RECENT_DAYS).with(LocalTime.MIN);

        for (Product product : products) {
            Integer currentStock = product.getTotalStock();
            if (currentStock == null || currentStock <= 0) continue;

            Integer sold90 = saleItemRepository.sumQuantityByProductIdAndStoreIdAndDateRange(
                    product.getId(), storeId, baselineStart, now);
            if (sold90 == null || sold90 <= 0) continue;

            Integer sold30 = saleItemRepository.sumQuantityByProductIdAndStoreIdAndDateRange(
                    product.getId(), storeId, recentStart, now);
            int recentQuantity = sold30 != null ? sold30 : 0;

            double avgMonthlyRate = sold90 / (VELOCITY_BASELINE_DAYS / 30.0);
            if (avgMonthlyRate <= 0 || recentQuantity >= avgMonthlyRate * SLOW_MOVER_THRESHOLD_RATIO) continue;

            recommendations.add(PricingRecommendationDTO.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productCode(product.getCode())
                    .currentStock(currentStock)
                    .reason("SLOW_MOVING")
                    .suggestedDiscountPercent(SLOW_MOVER_DISCOUNT_PERCENT)
                    .priority("LOW")
                    .message(String.format(
                            "Sold only %d unit(s) in the last %d days vs a %d-day average of %.1f/month",
                            recentQuantity, VELOCITY_RECENT_DAYS, VELOCITY_BASELINE_DAYS, avgMonthlyRate))
                    .build());
        }
        return recommendations;
    }

    private int priorityRank(String priority) {
        if (priority == null) return 3;
        return switch (priority) {
            case "HIGH" -> 0;
            case "MEDIUM" -> 1;
            case "LOW" -> 2;
            default -> 3;
        };
    }
}
