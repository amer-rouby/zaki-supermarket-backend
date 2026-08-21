// src/main/java/com/zakisupermarket/controller/DashboardController.java

package com.zakisupermarket.controller;

import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.DashboardResponse;
import com.zakisupermarket.dto.response.ZakiInsightsDTO;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.service.DashboardService;
import com.zakisupermarket.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboardStats(
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/dashboard/stats - storeId: {}", storeId);

        DashboardResponse stats = dashboardService.getDashboardStats(storeId);

        return ResponseEntity.ok(ApiResponse.success(
                stats,
                "Dashboard stats retrieved successfully"
        ));
    }

    @GetMapping("/zaki-insights")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<ZakiInsightsDTO>> getZakiInsights() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        try {
            ZakiInsightsDTO insights = dashboardService.getZakiInsights(storeId);
            return ResponseEntity.ok(ApiResponse.success(insights, "Zaki insights retrieved successfully"));
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting Zaki insights", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get Zaki insights: " + e.getMessage()));
        }
    }
}