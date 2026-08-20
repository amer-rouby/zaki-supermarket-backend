package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.StockAdjustmentRequest;
import com.zakisupermarket.dto.request.StockBatchRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.StockAdjustmentHistoryDTO;
import com.zakisupermarket.dto.response.StockBatchResponse;
import com.zakisupermarket.service.StockBatchService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock")
@RequiredArgsConstructor
@Slf4j
public class StockController {

    private final StockBatchService stockBatchService;

    @GetMapping("/batches")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockBatchResponse>>> getAllBatches(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/stock/batches - storeId: {}, page: {}, size: {}", storeId, page, size);

        Page<StockBatchResponse> batches = stockBatchService.getAllBatches(storeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(batches, "Stock batches retrieved successfully"));
    }

    @GetMapping("/batches/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockBatchResponse>> getBatch(
            @PathVariable Long id,
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/stock/batches/{} - storeId: {}", id, storeId);

        StockBatchResponse batch = stockBatchService.getBatch(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Stock batch retrieved successfully"));
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<StockBatchResponse>> createBatch(
            @Valid @RequestBody StockBatchRequest request,
            @RequestParam Long storeId,
            Authentication authentication) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("POST /api/stock/batches - storeId: {}, product: {}",
                storeId, request.getProductId());

        Long userId = SecurityUtils.extractUserId(authentication);
        StockBatchResponse batch = stockBatchService.createBatch(request, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Batch created successfully"));
    }

    @PutMapping("/batches/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<StockBatchResponse>> updateBatch(
            @PathVariable Long id,
            @Valid @RequestBody StockBatchRequest request,
            @RequestParam Long storeId,
            Authentication authentication) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("PUT /api/stock/batches/{} - storeId: {}", id, storeId);

        Long userId = SecurityUtils.extractUserId(authentication);
        StockBatchResponse batch = stockBatchService.updateBatch(id, request, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Batch updated successfully"));
    }

    @DeleteMapping("/batches/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteBatch(
            @PathVariable Long id,
            @RequestParam Long storeId,
            Authentication authentication) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("DELETE /api/stock/batches/{} - storeId: {}", id, storeId);

        Long userId = SecurityUtils.extractUserId(authentication);
        stockBatchService.deleteBatch(id, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Batch deleted successfully"));
    }

    @GetMapping("/expiring")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockBatchResponse>>> getExpiringBatches(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "30") int days) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/stock/expiring - storeId: {}, days: {}", storeId, days);

        List<StockBatchResponse> batches = stockBatchService.getExpiringBatches(storeId, days);
        return ResponseEntity.ok(ApiResponse.success(batches, "Expiring batches retrieved successfully"));
    }

    @GetMapping("/expired")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockBatchResponse>>> getExpiredBatches(
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/stock/expired - storeId: {}", storeId);

        List<StockBatchResponse> batches = stockBatchService.getExpiredBatches(storeId);
        return ResponseEntity.ok(ApiResponse.success(batches, "Expired batches retrieved successfully"));
    }

    @PostMapping("/batches/{id}/adjust")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<StockBatchResponse>> adjustStock(
            @PathVariable Long id,
            @Valid @RequestBody StockAdjustmentRequest request,
            Authentication authentication) {

        log.info("POST /api/stock/batches/{}/adjust - type: {}, quantity: {}",
                id, request.getType(), request.getQuantity());

        Long userId = SecurityUtils.extractUserId(authentication);
        Long storeId = SecurityUtils.getCurrentStoreId();
        StockBatchResponse batch = stockBatchService.adjustStock(id, request, userId, storeId);
        return ResponseEntity.ok(ApiResponse.success(batch, "Stock adjusted successfully"));
    }

    @GetMapping("/batches/{id}/adjustments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockAdjustmentHistoryDTO>>> getAdjustmentHistory(
            @PathVariable Long id,
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/stock/batches/{}/adjustments - storeId: {}", id, storeId);

        List<StockAdjustmentHistoryDTO> history = stockBatchService.getAdjustmentHistory(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(history, "Adjustment history retrieved successfully"));
    }

}