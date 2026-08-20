package com.zakisupermarket.dto.settings.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZakiFeatureSettingsRequest {

    private Boolean stockPredictionEnabled;
    private Boolean reorderRecommendationsEnabled;
    private Boolean pricingRecommendationsEnabled;
    private Boolean supplierRecommendationsEnabled;
    private Boolean dashboardInsightsEnabled;
    private Boolean dailyBriefEnabled;
    private Boolean anomalyDetectionEnabled;
    private Boolean realtimeUpdatesEnabled;
    private Boolean voiceSearchEnabled;
    private Boolean customerCreditEnabled;
    private Boolean aiAssistantEnabled;
    private Boolean eInvoiceEnabled;
    private Boolean offlineModeEnabled;
}
