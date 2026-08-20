package com.zakisupermarket.service.impl.settings;

import com.zakisupermarket.dto.settings.request.StoreSettingsRequest;
import com.zakisupermarket.dto.settings.response.StoreSettingsResponse;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.settings.StoreSettings;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.settings.StoreSettingsRepository;
import com.zakisupermarket.service.settings.StoreSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class StoreSettingsServiceImpl implements StoreSettingsService {

    private final StoreSettingsRepository settingsRepository;
    private final StoreRepository storeRepository;

    @Override
    @Transactional(readOnly = true)
    public StoreSettingsResponse getSettings(Long storeId) {
        StoreSettings settings = settingsRepository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultSettings(storeId));

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        return StoreSettingsResponse.fromEntity(settings, store.getName());
    }

    @Override
    @Transactional
    public StoreSettingsResponse updateSettings(Long storeId, StoreSettingsRequest request) {
        StoreSettings settings = settingsRepository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultSettings(storeId));

        settings.setAddress(request.getAddress());
        settings.setPhone(request.getPhone());
        settings.setEmail(request.getEmail());
        settings.setLicenseNumber(request.getLicenseNumber());
        settings.setTaxNumber(request.getTaxNumber());
        settings.setCommercialRegister(request.getCommercialRegister());
        settings.setLogoUrl(request.getLogoUrl());
        settings.setCurrency(request.getCurrency());
        settings.setTimezone(request.getTimezone());
        settings.setDateFormat(request.getDateFormat());
        settings.setTimeFormat(request.getTimeFormat());
        if (request.getEnabledPaymentMethods() != null && !request.getEnabledPaymentMethods().isBlank()) {
            settings.setEnabledPaymentMethods(request.getEnabledPaymentMethods());
        }
        if (request.getLargeSaleThreshold() != null) {
            settings.setLargeSaleThreshold(request.getLargeSaleThreshold());
        }
        if (request.getLargeExpenseThreshold() != null) {
            settings.setLargeExpenseThreshold(request.getLargeExpenseThreshold());
        }

        if (request.getStoreName() != null && !request.getStoreName().isBlank()) {
            Store store = storeRepository.findById(storeId)
                    .orElseThrow(() -> new RuntimeException("Store not found"));
            store.setName(request.getStoreName());
            storeRepository.save(store);
        }

        StoreSettings saved = settingsRepository.save(settings);
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        log.info("Store settings updated for storeId: {}", storeId);
        return StoreSettingsResponse.fromEntity(saved, store.getName());
    }

    private StoreSettings createDefaultSettings(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Store not found"));

        return StoreSettings.builder()
                .store(store)
                .currency("EGP")
                .timezone("Africa/Cairo")
                .dateFormat("dd/MM/yyyy")
                .timeFormat("24h")
                .build();
    }
}