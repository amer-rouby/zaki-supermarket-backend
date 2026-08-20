package com.zakisupermarket.controller.settings;

import com.zakisupermarket.dto.settings.request.StoreSettingsRequest;
import com.zakisupermarket.dto.settings.response.StoreSettingsResponse;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.service.settings.StoreSettingsService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/store")
@RequiredArgsConstructor
@Slf4j
public class StoreSettingsController {

    private final StoreSettingsService storeSettingsService;

    // Read-only store info (name, address, currency, etc.) needed on every screen
    // that formats money or prints an invoice - POS, sales history, sale details - not
    // just the settings screen itself, so any authenticated role can read it. Only
    // updateStoreSettings() below is the sensitive operation and stays ADMIN-only.
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER', 'VIEWER')")
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> getStoreSettings(
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/settings/store - storeId: {}", storeId);

        StoreSettingsResponse settings = storeSettingsService.getSettings(storeId);
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings retrieved successfully"));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<StoreSettingsResponse>> updateStoreSettings(
            @RequestParam Long storeId,
            @Valid @RequestBody StoreSettingsRequest request) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("PUT /api/settings/store - storeId: {}", storeId);

        StoreSettingsResponse settings = storeSettingsService.updateSettings(storeId, request);
        return ResponseEntity.ok(ApiResponse.success(settings, "Settings updated successfully"));
    }
}