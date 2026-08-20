package com.zakisupermarket.service.settings;

import com.zakisupermarket.dto.settings.request.ZakiFeatureSettingsRequest;
import com.zakisupermarket.dto.settings.response.ZakiFeatureSettingsResponse;
import com.zakisupermarket.entity.settings.ZakiFeatureSettings;

public interface ZakiFeatureSettingsService {

    ZakiFeatureSettingsResponse getSettings(Long storeId);

    ZakiFeatureSettingsResponse updateSettings(Long storeId, ZakiFeatureSettingsRequest request);

    // Used server-side by every Zaki feature's own controller/service to fail
    // closed when its flag is off, not just to render the settings screen.
    ZakiFeatureSettings getOrCreate(Long storeId);
}
