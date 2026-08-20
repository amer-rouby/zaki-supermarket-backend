package com.zakisupermarket.service.settings;

import com.zakisupermarket.dto.settings.request.StoreSettingsRequest;
import com.zakisupermarket.dto.settings.response.StoreSettingsResponse;

public interface StoreSettingsService {

    StoreSettingsResponse getSettings(Long storeId);

    StoreSettingsResponse updateSettings(Long storeId, StoreSettingsRequest request);
}