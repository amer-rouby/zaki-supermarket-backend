package com.zakisupermarket.controller;
import com.zakisupermarket.dto.response.AlertStatsResponse;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.StockAlertResponse;
import com.zakisupermarket.entity.StockAlert;
import com.zakisupermarket.service.StockAlertService;
import com.zakisupermarket.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Slf4j
public class StockAlertController {
    private final StockAlertService alertService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockAlertResponse>>> getAlerts(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("Getting alerts for store: {}, page: {}, size: {}", storeId, page, size);

        alertService.generateLowStockAlerts(storeId);
        alertService.generateExpiryAlerts(storeId);

        Page<StockAlertResponse> alerts = alertService.getAlerts(storeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<AlertStatsResponse>> getAlertStats(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("Getting alert stats for store: {}", storeId);

        alertService.generateLowStockAlerts(storeId);
        alertService.generateExpiryAlerts(storeId);

        AlertStatsResponse stats = alertService.getAlertStats(storeId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/active")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockAlertResponse>>> getActiveAlerts(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("Getting active alerts for store: {}", storeId);

        alertService.generateLowStockAlerts(storeId);
        alertService.generateExpiryAlerts(storeId);

        List<StockAlertResponse> alerts = alertService.getActiveAlerts(storeId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("Marking alert {} as read by user {}", id, userId);
        alertService.markAsRead(id, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Alert marked as read"));
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("Marking all alerts as read for store {} by user {}", storeId, userId);
        alertService.markAllAsRead(storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "All alerts marked as read"));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> resolveAlert(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("Resolving alert {} by user {}", id, userId);
        alertService.resolveAlert(id, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Alert resolved"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteAlert(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("Deleting alert {} for store {}", id, storeId);
        alertService.deleteAlert(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Alert deleted"));
    }

    @PostMapping("/generate/low-stock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> generateLowStockAlerts(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("Generating low stock alerts for store {}", storeId);
        alertService.generateLowStockAlerts(storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Low stock alerts generated"));
    }

    @PostMapping("/generate/expiry")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> generateExpiryAlerts(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("Generating expiry alerts for store {}", storeId);
        alertService.generateExpiryAlerts(storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Expiry alerts generated"));
    }
}