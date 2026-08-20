package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.PaymentRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.PaymentResponse;
import com.zakisupermarket.service.PaymentService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request) {

        request.setStoreId(SecurityUtils.getCurrentStoreId());
        log.info("Payment request received: {}", request);

        try {
            PaymentResponse response = paymentService.processPayment(request);

            return switch (response.getStatus()) {
                case "COMPLETED", "PENDING" ->
                        ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
                case "FAILED" ->
                        ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage()));
                default ->
                        ResponseEntity.status(422).body(ApiResponse.error(response.getMessage()));
            };

        } catch (Exception e) {
            log.error("Payment processing failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Payment failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{reference}/refund")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @PathVariable String reference,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false, defaultValue = "Customer request") String reason) {

        log.info("Refund request for: {}, amount: {}", reference, amount);
        try {
            PaymentResponse response = paymentService.refundPayment(reference, SecurityUtils.getCurrentStoreId(), amount, reason);
            return ResponseEntity.ok(ApiResponse.success(response, "Refund processed successfully"));
        } catch (Exception e) {
            log.error("Refund failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Refund failed: " + e.getMessage()));
        }
    }

    @PostMapping("/{reference}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PaymentResponse>> cancelPayment(
            @PathVariable String reference) {

        log.info("Cancel request for: {}", reference);
        try {
            PaymentResponse response = paymentService.cancelPayment(reference, SecurityUtils.getCurrentStoreId());
            return ResponseEntity.ok(ApiResponse.success(response, "Payment cancelled successfully"));
        } catch (Exception e) {
            log.error("Cancellation failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Cancellation failed: " + e.getMessage()));
        }
    }

    @GetMapping("/{reference}/verify")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @PathVariable String reference) {

        try {
            PaymentResponse response = paymentService.getPaymentByReference(reference, SecurityUtils.getCurrentStoreId());

            if (response == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(ApiResponse.success(response, "Payment verified"));

        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Verification failed: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> getPayments(
            @RequestParam Long storeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentMethod,
            @RequestParam(required = false) String search,
            Pageable pageable) {

        storeId = SecurityUtils.getCurrentStoreId();

        try {
            Page<PaymentResponse> payments = paymentService.getPaymentsByStore(
                    storeId, status, paymentMethod, search, pageable);
            return ResponseEntity.ok(ApiResponse.success(payments, "Payments retrieved successfully"));
        } catch (Exception e) {
            log.error("Get payments failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve payments: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPaymentStats(
            @RequestParam Long storeId) {

        storeId = SecurityUtils.getCurrentStoreId();

        try {
            Map<String, Object> stats = paymentService.getPaymentStats(storeId);
            return ResponseEntity.ok(ApiResponse.success(stats, "Stats retrieved successfully"));
        } catch (Exception e) {
            log.error("Get stats failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to retrieve stats: " + e.getMessage()));
        }
    }

    @GetMapping("/diagnose/gateways")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> diagnoseGateways() {
        Map<String, String> gateways = paymentService.getRegisteredGateways();
        return ResponseEntity.ok(ApiResponse.success(gateways, "Registered gateways"));
    }
}