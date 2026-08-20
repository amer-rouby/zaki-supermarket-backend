package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.UpdatePredictionDTO;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.DemandPredictionResponse;
import com.zakisupermarket.dto.response.ReorderRecommendationDTO;
import com.zakisupermarket.dto.response.SalesHistoryPointDTO;
import com.zakisupermarket.dto.response.SupplierReorderGroupDTO;
import com.zakisupermarket.dto.response.ShareLinkResponse;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.service.DemandPredictionService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
@Slf4j
public class DemandPredictionController {

    private final DemandPredictionService predictionService;

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<Void>> generatePredictions(
            @RequestParam Long storeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate forDate,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        try {
            Long userId = SecurityUtils.extractUserId(userDetails);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Invalid authentication"));
            }
            LocalDate targetDate = (forDate != null) ? forDate : LocalDate.now().plusDays(1);
            log.info("Generating predictions for store: {}, user: {}, date: {}", storeId, userId, targetDate);
            predictionService.generatePredictions(storeId, targetDate);
            return ResponseEntity.ok(ApiResponse.success(null, "Predictions generated for " + targetDate));
        } catch (Exception e) {
            log.error("Error generating predictions", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate predictions: " + e.getMessage()));
        }
    }

    @GetMapping("/upcoming")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<DemandPredictionResponse>>> getUpcomingPredictions(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "7") int daysAhead) {
        storeId = SecurityUtils.getCurrentStoreId();
        try {
            log.info("Getting upcoming predictions for store: {}, days: {}", storeId, daysAhead);
            List<DemandPredictionResponse> predictions = predictionService.getUpcomingPredictions(storeId, daysAhead);
            return ResponseEntity.ok(ApiResponse.success(predictions));
        } catch (Exception e) {
            log.error("Error getting upcoming predictions", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get predictions: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<DemandPredictionResponse>>> getPredictions(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        storeId = SecurityUtils.getCurrentStoreId();
        try {
            log.info("Getting predictions for store: {}, page: {}, size: {}", storeId, page, size);
            Page<DemandPredictionResponse> predictions = predictionService.getPredictions(storeId, page, size);
            return ResponseEntity.ok(ApiResponse.success(predictions));
        } catch (Exception e) {
            log.error("Error getting predictions", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get predictions: " + e.getMessage()));
        }
    }

    @GetMapping("/accuracy")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccuracyStats(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        try {
            log.info("Getting accuracy stats for store: {}", storeId);
            Map<String, Object> stats = predictionService.getAccuracyStats(storeId);
            return ResponseEntity.ok(ApiResponse.success(stats));
        } catch (Exception e) {
            log.error("Error getting accuracy stats", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get stats: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}/actual")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<Void>> updateActualQuantity(
            @PathVariable Long id,
            @RequestParam Integer actualQuantity,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long userId = SecurityUtils.extractUserId(userDetails);
            if (userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Invalid authentication"));
            }
            Long storeId = SecurityUtils.getCurrentStoreId();
            log.info("Updating actual quantity for prediction: {}, actual: {}, user: {}", id, actualQuantity, userId);
            predictionService.updatePredictionWithActual(id, actualQuantity, storeId);
            return ResponseEntity.ok(ApiResponse.success(null, "Actual quantity updated"));
        } catch (Exception e) {
            log.error("Error updating actual quantity", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<DemandPredictionResponse>> updatePrediction(
            @PathVariable Long id,
            @RequestBody UpdatePredictionDTO updates,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long storeId = SecurityUtils.extractStoreId(userDetails);
            if (storeId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Invalid authentication"));
            }
            log.info("Updating prediction: {}, user: {}", id, userDetails.getUsername());
            DemandPredictionResponse updated = predictionService.updatePrediction(id, updates, storeId);
            return ResponseEntity.ok(ApiResponse.success(updated, "Prediction updated successfully"));
        } catch (Exception e) {
            log.error("Error updating prediction", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to update: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<Void>> deletePrediction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long storeId = SecurityUtils.extractStoreId(userDetails);
            if (storeId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Invalid authentication"));
            }
            log.info("Deleting prediction: {}, user: {}", id, userDetails.getUsername());
            predictionService.deletePrediction(id, storeId);
            return ResponseEntity.ok(ApiResponse.success(null, "Prediction deleted successfully"));
        } catch (Exception e) {
            log.error("Error deleting prediction", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to delete: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/export/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportPredictionPdf(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long storeId = SecurityUtils.extractStoreId(userDetails);
            if (storeId == null) {
                return ResponseEntity.status(401).build();
            }
            byte[] pdf = predictionService.exportPredictionToPdf(id, storeId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prediction_" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (Exception e) {
            log.error("Error exporting prediction to PDF", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/export/excel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> exportPredictionExcel(@PathVariable Long id,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long storeId = SecurityUtils.extractStoreId(userDetails);
            if (storeId == null) {
                return ResponseEntity.status(401).build();
            }
            byte[] excel = predictionService.exportPredictionToExcel(id, storeId);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=prediction_" + id + ".xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excel);
        } catch (Exception e) {
            log.error("Error exporting prediction to Excel", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{id}/share")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ShareLinkResponse>> sharePrediction(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> requestBody,
            @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Long storeId = SecurityUtils.extractStoreId(userDetails);
            Long userId = SecurityUtils.extractUserId(userDetails);
            if (storeId == null || userId == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Invalid authentication"));
            }
            int expiryHours = 24;
            if (requestBody != null && requestBody.containsKey("expiryHours")) {
                expiryHours = Integer.parseInt(requestBody.get("expiryHours").toString());
            }
            log.info("Generating share link for prediction: {}, expiry: {}h", id, expiryHours);
            ShareLinkResponse shareLink = predictionService.generateShareLink(id, storeId, userId, expiryHours);
            return ResponseEntity.ok(ApiResponse.success(shareLink, "Share link generated"));
        } catch (Exception e) {
            log.error("Error generating share link", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to generate share link: " + e.getMessage()));
        }
    }

    @GetMapping("/sales-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SalesHistoryPointDTO>>> getProductSalesHistory(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "30") int days) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        try {
            List<SalesHistoryPointDTO> history = predictionService.getProductSalesHistory(productId, storeId, days);
            return ResponseEntity.ok(ApiResponse.success(history));
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting product sales history", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get sales history: " + e.getMessage()));
        }
    }

    @GetMapping("/reorder-recommendations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ReorderRecommendationDTO>>> getReorderRecommendations() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        try {
            List<ReorderRecommendationDTO> recommendations = predictionService.getReorderRecommendations(storeId);
            return ResponseEntity.ok(ApiResponse.success(recommendations));
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting reorder recommendations", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get reorder recommendations: " + e.getMessage()));
        }
    }

    @GetMapping("/reorder-recommendations/by-supplier")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SupplierReorderGroupDTO>>> getReorderRecommendationsBySupplier() {
        Long storeId = SecurityUtils.getCurrentStoreId();
        try {
            List<SupplierReorderGroupDTO> groups = predictionService.getReorderRecommendationsBySupplier(storeId);
            return ResponseEntity.ok(ApiResponse.success(groups));
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting supplier reorder recommendations", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get supplier reorder recommendations: " + e.getMessage()));
        }
    }

}