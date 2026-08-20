package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.SaleRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.SaleTransactionDTO;
import com.zakisupermarket.dto.response.SalesReportResponse;
import com.zakisupermarket.service.SaleTransactionService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
@Slf4j
public class SalesController {

    private final SaleTransactionService saleTransactionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<SaleTransactionDTO>>> getAllSales(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transactionDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection) {

        storeId = SecurityUtils.getCurrentStoreId();

        log.info("GET /api/sales - storeId: {}, page: {}, size: {}, sortBy: {}, sortDirection: {}",
                storeId, page, size, sortBy, sortDirection);

        Page<SaleTransactionDTO> sales = saleTransactionService.getAllSales(
                storeId, page, size, sortBy, sortDirection);

        log.info("Returning {} sales (totalElements: {}, totalPages: {})",
                sales.getContent().size(), sales.getTotalElements(), sales.getTotalPages());

        return ResponseEntity.ok(ApiResponse.success(sales, "Sales retrieved successfully"));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<SaleTransactionDTO>> getSale(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/sales/{} - storeId: {}", id, storeId);
        SaleTransactionDTO sale = saleTransactionService.getSaleById(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(sale, "Sale retrieved successfully"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<SaleTransactionDTO>> createSale(
            @Valid @RequestBody SaleRequest request,
            @RequestParam Long storeId,
            Authentication authentication) {
        storeId = SecurityUtils.getCurrentStoreId();
        request.setStoreId(storeId);
        log.info("POST /api/sales - storeId: {}, items: {}", storeId, request.getItems().size());

        Long currentUserId = SecurityUtils.extractUserId(authentication);

        SaleTransactionDTO response = createSaleWithRetry(request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Sale created successfully"));
    }

    /**
     * Two cashiers selling the last units of the same batch at the same time can race:
     * both read the same quantityCurrent before either writes back. StockBatch now has
     * an @Version column, so the second writer's transaction fails with an optimistic
     * lock exception instead of silently overselling. Retrying re-reads the batch with
     * its now-current quantity, so this resolves itself transparently for the caller in
     * all but pathological contention.
     */
    private SaleTransactionDTO createSaleWithRetry(SaleRequest request, Long currentUserId) {
        final int maxAttempts = 3;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return saleTransactionService.createSale(request, currentUserId);
            } catch (OptimisticLockingFailureException ex) {
                if (attempt == maxAttempts) {
                    log.warn("Sale creation failed after {} attempts due to concurrent stock updates", maxAttempts);
                    throw new RuntimeException("This item was just sold by another transaction. Please try again.");
                }
                log.debug("Concurrent stock update detected, retrying sale creation (attempt {}/{})", attempt, maxAttempts);
            }
        }
        throw new IllegalStateException("unreachable");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<SaleTransactionDTO>> updateSale(
            @PathVariable Long id,
            @Valid @RequestBody SaleRequest request,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("PUT /api/sales/{} - storeId: {}", id, storeId);
        SaleTransactionDTO response = saleTransactionService.updateSale(id, request, storeId);
        return ResponseEntity.ok(ApiResponse.success(response, "Sale updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSale(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("DELETE /api/sales/{} - storeId: {}", id, storeId);
        saleTransactionService.deleteSale(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Sale deleted successfully"));
    }

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesAnalytics(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "monthly") String period) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/sales/analytics - storeId: {}, startDate: {}, endDate: {}, period: {}",
                storeId, startDate, endDate, period);

        SalesReportResponse analytics = saleTransactionService.getSalesAnalytics(storeId, startDate, endDate, period);
        return ResponseEntity.ok(ApiResponse.success(analytics, "Analytics retrieved successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesStats(@RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/sales/stats - storeId: {}", storeId);
        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getSalesStats(storeId),
                "Sales stats retrieved successfully"));
    }

    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySales(@RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/sales/today - storeId: {}", storeId);
        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getTodaySales(storeId),
                "Today sales retrieved successfully"));
    }

    @GetMapping("/today/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySalesSummary(@RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/sales/today/summary - storeId: {}", storeId);
        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getTodaySales(storeId),
                "Today sales summary retrieved successfully"));
    }

    @GetMapping("/range")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<SaleTransactionDTO>>> getSalesByDateRange(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/sales/range - storeId: {}, startDate: {}, endDate: {}",
                storeId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getSalesByDateRange(storeId, startDate, endDate),
                "Sales by date range retrieved successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Page<SaleTransactionDTO>>> searchSales(
            @RequestParam Long storeId,
            @RequestParam String query) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/sales/search - storeId: {}, query: {}", storeId, query);

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.searchSales(storeId, query),
                "Search completed successfully"));
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<SaleTransactionDTO>>> getRecentSales(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "10") int limit) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/sales/recent - storeId: {}, limit: {}", storeId, limit);

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getRecentSales(storeId, limit),
                "Recent sales retrieved successfully"));
    }

    @GetMapping("/by-category")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSalesByCategory(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/sales/by-category - storeId: {}, startDate: {}, endDate: {}",
                storeId, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getSalesByCategory(storeId, startDate, endDate),
                "Sales by category retrieved successfully"));
    }

    @GetMapping("/top-products")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopProducts(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "10") int limit) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/sales/top-products - storeId: {}, limit: {}", storeId, limit);

        return ResponseEntity.ok(ApiResponse.success(
                saleTransactionService.getTopProducts(storeId, limit),
                "Top products retrieved successfully"));
    }
}