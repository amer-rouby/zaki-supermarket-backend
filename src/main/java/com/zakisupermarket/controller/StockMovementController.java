package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.StockMovementRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.StockMovementResponse;
import com.zakisupermarket.dto.response.StockMovementStats;
import com.zakisupermarket.entity.StockMovement;
import com.zakisupermarket.service.StockMovementService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/stock/movements")
@RequiredArgsConstructor
@Slf4j
public class StockMovementController {

    private final StockMovementService movementService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockMovementResponse>> createMovement(
            @Valid @RequestBody StockMovementRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Long userId = SecurityUtils.extractUserId(userDetails);
        Long storeId = SecurityUtils.getCurrentStoreId();
        log.info("Creating stock movement for user: {}", userId);

        StockMovementResponse movement = movementService.createMovement(request, userId, storeId);
        return ResponseEntity.ok(ApiResponse.success(movement, "Movement created successfully"));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockMovementResponse>>> getMovementsByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        storeId = SecurityUtils.getCurrentStoreId();
        log.info("Getting movements for store: {}", storeId);

        Page<StockMovementResponse> movements = movementService.getMovementsByStore(storeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    @GetMapping("/batch/{batchId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockMovementResponse>>> getMovementsByBatch(
            @PathVariable Long batchId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Long storeId = SecurityUtils.getCurrentStoreId();
        log.info("Getting movements for batch: {}", batchId);

        Page<StockMovementResponse> movements = movementService.getMovementsByBatch(batchId, storeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    @GetMapping("/date-range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<StockMovementResponse>>> getMovementsByDateRange(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) StockMovement.MovementType movementType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("Getting movements for store: {} from {} to {} type {}", storeId, startDate, endDate, movementType);

        Page<StockMovementResponse> movements = movementService.getMovementsByDateRange(storeId, startDate, endDate, movementType, page, size);
        return ResponseEntity.ok(ApiResponse.success(movements));
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<StockMovementStats>> getMovementStats(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("Getting movement stats for store: {} from {} to {}", storeId, startDate, endDate);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999999999);

        log.info("Querying from {} to {}", startDateTime, endDateTime);

        StockMovementStats stats = movementService.getMovementStats(storeId, startDateTime, endDateTime);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}