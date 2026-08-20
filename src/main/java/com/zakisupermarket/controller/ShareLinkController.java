package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.CreateShareLinkRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.ShareLinkResponse;
import com.zakisupermarket.entity.ShareLink;
import com.zakisupermarket.service.DemandPredictionService;
import com.zakisupermarket.service.ShareLinkService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
@Slf4j
public class ShareLinkController {

    private final ShareLinkService shareLinkService;
    private final DemandPredictionService demandPredictionService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> createShareLink(
            @Valid @RequestBody CreateShareLinkRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = SecurityUtils.extractUserId(userDetails);
        Long storeId = SecurityUtils.extractStoreId(userDetails);

        log.info("Creating share link for entityType: {}, entityId: {}",
                request.getEntityType(), request.getEntityId());

        ShareLinkResponse response = shareLinkService.createShareLink(
                request, userId, storeId);

        return ResponseEntity.ok(ApiResponse.success(response, "Share link generated"));
    }

    @GetMapping("/{token}")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> getSharedData(
            @PathVariable String token) {

        log.info("Accessing shared data with token: {}", token);

        ShareLink shareLink = shareLinkService.validateShareLink(token);
        shareLinkService.incrementAccessCount(token);

        Object data = fetchSharedEntityData(shareLink);

        return ResponseEntity.ok(ApiResponse.success(
                ShareLinkResponse.builder()
                        .shareUrl(shareLinkService.buildShareUrl(shareLink))
                        .token(token)
                        .expiresAt(shareLink.getExpiresAt())
                        .entityType(shareLink.getEntityType())
                        .entityId(shareLink.getEntityId())
                        .data(data)
                        .build(),
                "Shared data retrieved successfully"));
    }

    /**
     * Dispatches on entityType to fetch the actual shared payload. This endpoint is
     * public (no auth), so it always scopes the lookup to shareLink.getStoreId() -
     * the store that created the link, never anything client-supplied.
     */
    private Object fetchSharedEntityData(ShareLink shareLink) {
        String entityType = shareLink.getEntityType();
        if (entityType == null) {
            return null;
        }
        if ("prediction".equalsIgnoreCase(entityType)) {
            return demandPredictionService.getPredictionById(shareLink.getEntityId(), shareLink.getStoreId());
        }
        log.warn("Share link {} has unsupported entityType '{}' - returning metadata only, no data payload",
                shareLink.getToken(), entityType);
        return null;
    }
}
