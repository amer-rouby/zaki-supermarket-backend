package com.zakisupermarket.service.impl.settings;

import com.zakisupermarket.dto.settings.request.ZakiFeatureSettingsRequest;
import com.zakisupermarket.dto.settings.response.ZakiFeatureSettingsResponse;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.settings.ZakiFeatureSettings;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.settings.ZakiFeatureSettingsRepository;
import com.zakisupermarket.service.settings.ZakiFeatureSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZakiFeatureSettingsServiceImpl implements ZakiFeatureSettingsService {

    private final ZakiFeatureSettingsRepository settingsRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public ZakiFeatureSettingsResponse getSettings(Long storeId) {
        return ZakiFeatureSettingsResponse.fromEntity(getOrCreate(storeId));
    }

    @Override
    @Transactional
    public ZakiFeatureSettingsResponse updateSettings(Long storeId, ZakiFeatureSettingsRequest request) {
        ZakiFeatureSettings settings = getOrCreate(storeId);

        if (request.getStockPredictionEnabled() != null) {
            settings.setStockPredictionEnabled(request.getStockPredictionEnabled());
        }
        if (request.getReorderRecommendationsEnabled() != null) {
            settings.setReorderRecommendationsEnabled(request.getReorderRecommendationsEnabled());
        }
        if (request.getPricingRecommendationsEnabled() != null) {
            settings.setPricingRecommendationsEnabled(request.getPricingRecommendationsEnabled());
        }
        if (request.getSupplierRecommendationsEnabled() != null) {
            settings.setSupplierRecommendationsEnabled(request.getSupplierRecommendationsEnabled());
        }
        if (request.getDashboardInsightsEnabled() != null) {
            settings.setDashboardInsightsEnabled(request.getDashboardInsightsEnabled());
        }
        if (request.getDailyBriefEnabled() != null) {
            settings.setDailyBriefEnabled(request.getDailyBriefEnabled());
        }
        if (request.getAnomalyDetectionEnabled() != null) {
            settings.setAnomalyDetectionEnabled(request.getAnomalyDetectionEnabled());
        }
        if (request.getRealtimeUpdatesEnabled() != null) {
            settings.setRealtimeUpdatesEnabled(request.getRealtimeUpdatesEnabled());
        }
        if (request.getVoiceSearchEnabled() != null) {
            settings.setVoiceSearchEnabled(request.getVoiceSearchEnabled());
        }
        if (request.getCustomerCreditEnabled() != null) {
            settings.setCustomerCreditEnabled(request.getCustomerCreditEnabled());
        }
        if (request.getAiAssistantEnabled() != null) {
            settings.setAiAssistantEnabled(request.getAiAssistantEnabled());
        }
        if (request.getEInvoiceEnabled() != null) {
            settings.setEInvoiceEnabled(request.getEInvoiceEnabled());
        }
        if (request.getOfflineModeEnabled() != null) {
            settings.setOfflineModeEnabled(request.getOfflineModeEnabled());
        }

        ZakiFeatureSettings saved = settingsRepository.save(settings);
        log.info("Zaki feature settings updated for storeId: {}", storeId);
        return ZakiFeatureSettingsResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public ZakiFeatureSettings getOrCreate(Long storeId) {
        return settingsRepository.findByStoreId(storeId)
                .orElseGet(() -> buildDefaultSettings(storeId));
    }

    // Deliberately not persisted here - getSettings() is read-only, and a plain
    // GET shouldn't have a write side effect. The transient defaults get saved
    // for real the first time updateSettings() actually runs.
    private ZakiFeatureSettings buildDefaultSettings(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        return ZakiFeatureSettings.builder()
                .store(store)
                .build();
    }
}
