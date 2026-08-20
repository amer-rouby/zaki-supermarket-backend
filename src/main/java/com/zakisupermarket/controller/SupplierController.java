package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.SupplierRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.SupplierResponse;
import com.zakisupermarket.service.SupplierService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
@Slf4j
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getAllSuppliers(
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/suppliers - storeId: {}", storeId);
        List<SupplierResponse> suppliers = supplierService.getAllSuppliers(storeId);
        return ResponseEntity.ok(ApiResponse.success(suppliers));
    }

    @GetMapping("/paginated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<SupplierResponse>>> getSuppliersPaginated(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/suppliers/paginated - storeId: {}, page: {}, size: {}", storeId, page, size);
        Page<SupplierResponse> suppliers = supplierService.getSuppliersPaginated(storeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(suppliers));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SupplierResponse>> getSupplier(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/suppliers/{} - storeId: {}", id, storeId);
        SupplierResponse supplier = supplierService.getSupplier(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(supplier));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
            @Valid @RequestBody SupplierRequest request,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("POST /api/suppliers - storeId: {}, userId: {}", storeId, userId);
        SupplierResponse supplier = supplierService.createSupplier(request, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(supplier, "Supplier created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("PUT /api/suppliers/{} - storeId: {}, userId: {}", id, storeId, userId);
        SupplierResponse supplier = supplierService.updateSupplier(id, request, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(supplier, "Supplier updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteSupplier(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("DELETE /api/suppliers/{} - storeId: {}, userId: {}", id, storeId, userId);
        supplierService.deleteSupplier(id, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Supplier deleted successfully"));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countSuppliers(@RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long count = supplierService.countSuppliers(storeId);
        Map<String, Long> response = new HashMap<>();
        response.put("count", count);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> searchSuppliers(
            @RequestParam Long storeId,
            @RequestParam String query) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/suppliers/search - storeId: {}, query: {}", storeId, query);
        List<SupplierResponse> suppliers = supplierService.searchSuppliers(storeId, query);
        return ResponseEntity.ok(ApiResponse.success(suppliers));
    }
}