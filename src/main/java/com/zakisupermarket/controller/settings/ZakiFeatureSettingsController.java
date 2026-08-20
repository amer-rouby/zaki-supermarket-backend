package com.zakisupermarket.controller.settings;

import com.zakisupermarket.dto.settings.request.ZakiFeatureSettingsRequest;
import com.zakisupermarket.dto.settings.response.ZakiFeatureSettingsResponse;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.service.settings.ZakiFeatureSettingsService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/zaki-features")
@RequiredArgsConstructor
@Slf4j
public class ZakiFeatureSettingsController {

    private final ZakiFeatureSettingsService zakiFeatureSettingsService;

    // Any authenticated role can read the flags - every screen gated by one needs
    // to know whether to render itself, not just the settings screen. Only the
    // PUT below (the actual toggle) is ADMIN-only.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ZakiFeatureSettingsResponse>> getSettings(
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/settings/zaki-features - storeId: {}", storeId);

        ZakiFeatureSettingsResponse settings = zakiFeatureSettingsService.getSettings(storeId);
        return ResponseEntity.ok(ApiResponse.success(settings, "Zaki feature settings retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ZakiFeatureSettingsResponse>> updateSettings(
            @RequestParam Long storeId,
            @Valid @RequestBody ZakiFeatureSettingsRequest request) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("PUT /api/settings/zaki-features - storeId: {}", storeId);

        ZakiFeatureSettingsResponse settings = zakiFeatureSettingsService.updateSettings(storeId, request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Zaki feature settings updated successfully"));
    }
}
