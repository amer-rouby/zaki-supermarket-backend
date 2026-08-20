package com.zakisupermarket.service.settings;

import com.zakisupermarket.dto.settings.request.NotificationSettingsRequest;
import com.zakisupermarket.dto.settings.response.NotificationSettingsResponse;

public interface NotificationSettingsService {

    NotificationSettingsResponse getSettings(Long userId);

    NotificationSettingsResponse updateSettings(Long userId, NotificationSettingsRequest request);
}