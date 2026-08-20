package com.zakisupermarket.dto.settings.response;

import com.zakisupermarket.entity.settings.ZakiFeatureSettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZakiFeatureSettingsResponse {

    private Long id;
    private Long storeId;
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
    private LocalDateTime updatedAt;

    public static ZakiFeatureSettingsResponse fromEntity(ZakiFeatureSettings settings) {
        return ZakiFeatureSettingsResponse.builder()
                .id(settings.getId())
                .storeId(settings.getStore().getId())
                .stockPredictionEnabled(settings.getStockPredictionEnabled())
                .reorderRecommendationsEnabled(settings.getReorderRecommendationsEnabled())
                .pricingRecommendationsEnabled(settings.getPricingRecommendationsEnabled())
                .supplierRecommendationsEnabled(settings.getSupplierRecommendationsEnabled())
                .dashboardInsightsEnabled(settings.getDashboardInsightsEnabled())
                .dailyBriefEnabled(settings.getDailyBriefEnabled())
                .anomalyDetectionEnabled(settings.getAnomalyDetectionEnabled())
                .realtimeUpdatesEnabled(settings.getRealtimeUpdatesEnabled())
                .voiceSearchEnabled(settings.getVoiceSearchEnabled())
                .customerCreditEnabled(settings.getCustomerCreditEnabled())
                .aiAssistantEnabled(settings.getAiAssistantEnabled())
                .eInvoiceEnabled(settings.getEInvoiceEnabled())
                .offlineModeEnabled(settings.getOfflineModeEnabled())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
