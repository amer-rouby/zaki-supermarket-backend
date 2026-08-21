package com.zakisupermarket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZakiInsightsDTO {
    private BigDecimal todayRevenue;
    private BigDecimal averageDailyRevenue30d;
    private Double salesDeltaPercent;

    // Each of these is null when its underlying Zaki feature is disabled
    // for the store, rather than shown as zero - "no data" vs "off".
    private Integer highRiskStockoutCount;
    private Integer reorderRecommendationsCount;
    private Integer pricingRecommendationsCount;
}
