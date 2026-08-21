package com.zakisupermarket.controller;

import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.EInvoiceSubmissionResponse;
import com.zakisupermarket.exception.FeatureDisabledException;
import com.zakisupermarket.service.EInvoiceService;
import com.zakisupermarket.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/e-invoice")
@RequiredArgsConstructor
@Slf4j
public class EInvoiceController {

    private final EInvoiceService eInvoiceService;

    @GetMapping("/{saleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EInvoiceSubmissionResponse>> getForSale(@PathVariable Long saleId) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        try {
            EInvoiceSubmissionResponse response = eInvoiceService.getForSale(saleId, storeId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error getting e-invoice submission", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to get e-invoice status: " + e.getMessage()));
        }
    }

    @PostMapping("/{saleId}/submit")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EInvoiceSubmissionResponse>> submit(@PathVariable Long saleId) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        try {
            EInvoiceSubmissionResponse response = eInvoiceService.submit(saleId, storeId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error submitting e-invoice", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to submit e-invoice: " + e.getMessage()));
        }
    }

    @PostMapping("/{saleId}/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<EInvoiceSubmissionResponse>> retry(@PathVariable Long saleId) {
        Long storeId = SecurityUtils.getCurrentStoreId();
        try {
            EInvoiceSubmissionResponse response = eInvoiceService.retry(saleId, storeId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (FeatureDisabledException e) {
            return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error retrying e-invoice", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retry e-invoice: " + e.getMessage()));
        }
    }
}
