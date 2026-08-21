package com.zakisupermarket.controller;

import com.zakisupermarket.dto.response.AnomalyDetectionResponse;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.entity.AnomalyDetection;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.service.AnomalyDetectionService;
import com.zakisupermarket.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionController {

    private final AnomalyDetectionService anomalyDetectionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<AnomalyDetectionResponse>>> getAnomalies(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        try {
            AnomalyDetection.Status statusEnum = status != null ? AnomalyDetection.Status.valueOf(status) : null;
            AnomalyDetection.Type typeEnum = type != null ? AnomalyDetection.Type.valueOf(type) : null;
            Page<AnomalyDetectionResponse> result = anomalyDetectionService.getAnomalies(storeId, statusEnum, typeEnum, page, size);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting anomalies", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get anomalies: " + e.getMessage()));
        }
    }

    @GetMapping("/counts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCounts() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        try {
            Map<String, Long> counts = Map.of(
                    "NEW", anomalyDetectionService.countByStatus(storeId, AnomalyDetection.Status.NEW),
                    "REVIEWED", anomalyDetectionService.countByStatus(storeId, AnomalyDetection.Status.REVIEWED),
                    "DISMISSED", anomalyDetectionService.countByStatus(storeId, AnomalyDetection.Status.DISMISSED)
            );
            return ResponseEntity.ok(ApiResponse.success(counts));
        } catch (Exception e) {
            log.error("Error getting anomaly counts", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get anomaly counts: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnomalyDetectionResponse>> markReviewed(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        try {
            AnomalyDetectionResponse response = anomalyDetectionService.markReviewed(id, storeId, userId);
            return ResponseEntity.ok(ApiResponse.success(response, "Marked as reviewed"));
        } catch (Exception e) {
            log.error("Error marking anomaly reviewed", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update anomaly: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/dismiss")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<AnomalyDetectionResponse>> dismiss(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        try {
            AnomalyDetectionResponse response = anomalyDetectionService.dismiss(id, storeId, userId);
            return ResponseEntity.ok(ApiResponse.success(response, "Dismissed"));
        } catch (Exception e) {
            log.error("Error dismissing anomaly", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update anomaly: " + e.getMessage()));
        }
    }

    @PostMapping("/detect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> triggerDetection() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        try {
            anomalyDetectionService.runDetectionForStore(storeId);
            return ResponseEntity.ok(ApiResponse.success(null, "Anomaly detection run completed"));
        } catch (Exception e) {
            log.error("Error running anomaly detection", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to run detection: " + e.getMessage()));
        }
    }
}
