package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.CustomerPaymentRequest;
import com.zakisupermarket.dto.request.CustomerRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.CustomerResponse;
import com.zakisupermarket.dto.response.CustomerStatementResponse;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.service.CustomerService;
import com.zakisupermarket.service.settings.ZakiFeatureSettingsService;
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

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
@Slf4j
public class CustomerController {

    private final CustomerService customerService;
    private final ZakiFeatureSettingsService zakiFeatureSettingsService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> getAllCustomers(@RequestParam Long storeId) {
        Long resolvedStoreId = SecurityUtils.getCurrentStoreId();
        try {
            ensureEnabled(resolvedStoreId);
            return ResponseEntity.ok(ApiResponse.success(customerService.getAllCustomers(resolvedStoreId)));
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/paginated")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<CustomerResponse>>> getCustomersPaginated(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long resolvedStoreId = SecurityUtils.getCurrentStoreId();
        try {
            ensureEnabled(resolvedStoreId);
            return ResponseEntity.ok(ApiResponse.success(customerService.getCustomersPaginated(resolvedStoreId, page, size)));
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CustomerResponse>> getCustomer(
            @PathVariable Long id, @RequestParam Long storeId) {
        Long resolvedStoreId = SecurityUtils.getCurrentStoreId();
        return ResponseEntity.ok(ApiResponse.success(customerService.getCustomer(id, resolvedStoreId)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<CustomerResponse>> createCustomer(
            @Valid @RequestBody CustomerRequest request, @RequestParam Long storeId) {
        Long resolvedStoreId = SecurityUtils.getCurrentStoreId();
        CustomerResponse customer = customerService.createCustomer(request, resolvedStoreId);
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer created successfully"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateCustomer(
            @PathVariable Long id, @Valid @RequestBody CustomerRequest request, @RequestParam Long storeId) {
        Long resolvedStoreId = SecurityUtils.getCurrentStoreId();
        CustomerResponse customer = customerService.updateCustomer(id, request, resolvedStoreId);
        return ResponseEntity.ok(ApiResponse.success(customer, "Customer updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id, @RequestParam Long storeId) {
        Long resolvedStoreId = SecurityUtils.getCurrentStoreId();
        customerService.deleteCustomer(id, resolvedStoreId);
        return ResponseEntity.ok(ApiResponse.success(null, "Customer deleted successfully"));
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CustomerResponse>>> searchCustomers(
            @RequestParam Long storeId, @RequestParam String query) {
        Long resolvedStoreId = SecurityUtils.getCurrentStoreId();
        return ResponseEntity.ok(ApiResponse.success(customerService.searchCustomers(resolvedStoreId, query)));
    }

    @GetMapping("/{id}/statement")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<CustomerStatementResponse>> getStatement(
            @PathVariable Long id, @RequestParam Long storeId) {
        Long resolvedStoreId = SecurityUtils.getCurrentStoreId();
        return ResponseEntity.ok(ApiResponse.success(customerService.getStatement(id, resolvedStoreId)));
    }

    @PostMapping("/{id}/payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<CustomerResponse>> recordPayment(
            @PathVariable Long id,
            @Valid @RequestBody CustomerPaymentRequest request,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long resolvedStoreId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        CustomerResponse customer = customerService.recordPayment(id, request, resolvedStoreId, userId);
        return ResponseEntity.ok(ApiResponse.success(customer, "Payment recorded successfully"));
    }

    private void ensureEnabled(Long storeId) {
        Boolean enabled = zakiFeatureSettingsService.getOrCreate(storeId).getCustomerCreditEnabled();
        if (enabled != null && !enabled) {
            throw new FeatureDisabledException("FEATURE_DISABLED_CUSTOMER_CREDIT", "Customer credit management feature is disabled for this store");
        }
    }
}
