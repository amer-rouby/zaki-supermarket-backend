package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.PurchaseOrderRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.PurchaseOrderResponse;
import com.zakisupermarket.dto.response.SendEmailResponse;
import com.zakisupermarket.dto.response.SendWhatsAppResponse;
import com.zakisupermarket.dto.response.WhatsAppMessageResponse;
import com.zakisupermarket.service.PurchaseOrderService;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/purchase-orders")
@RequiredArgsConstructor
@Slf4j
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderResponse>>> getAllOrders(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/purchase-orders - storeId: {}, page: {}, size: {}", storeId, page, size);
        Page<PurchaseOrderResponse> orders = purchaseOrderService.getAllOrders(storeId, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<PurchaseOrderResponse>>> getOrdersByStatus(
            @RequestParam Long storeId,
            @PathVariable String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/purchase-orders/status/{} - storeId: {}", status, storeId);
        Page<PurchaseOrderResponse> orders = purchaseOrderService.getOrdersByStatus(storeId, status, page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getOrder(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/purchase-orders/{} - storeId: {}", id, storeId);
        PurchaseOrderResponse order = purchaseOrderService.getOrder(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createOrder(
            @Valid @RequestBody PurchaseOrderRequest request,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("POST /api/purchase-orders - storeId: {}, userId: {}", storeId, userId);
        PurchaseOrderResponse order = purchaseOrderService.createOrder(request, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order created successfully"));
    }

    @PostMapping("/from-prediction/{predictionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createFromPrediction(
            @PathVariable Long predictionId,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("POST /api/purchase-orders/from-prediction/{} - storeId: {}", predictionId, storeId);
        PurchaseOrderResponse order = purchaseOrderService.createFromPrediction(predictionId, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order created from prediction"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updateOrder(
            @PathVariable Long id,
            @Valid @RequestBody PurchaseOrderRequest request,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("PUT /api/purchase-orders/{} - storeId: {}", id, storeId);
        PurchaseOrderResponse order = purchaseOrderService.updateOrder(id, request, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order updated successfully"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteOrder(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("DELETE /api/purchase-orders/{} - storeId: {}", id, storeId);
        purchaseOrderService.deleteOrder(id, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Purchase order deleted successfully"));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> approveOrder(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("POST /api/purchase-orders/{}/approve - storeId: {}", id, storeId);
        PurchaseOrderResponse order = purchaseOrderService.approveOrder(id, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order approved"));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> cancelOrder(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("POST /api/purchase-orders/{}/cancel - storeId: {}", id, storeId);
        PurchaseOrderResponse order = purchaseOrderService.cancelOrder(id, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order cancelled"));
    }

    @PostMapping("/{id}/receive")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACIST')")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receiveOrder(
            @PathVariable Long id,
            @RequestParam Long storeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.extractUserId(userDetails);
        log.info("POST /api/purchase-orders/{}/receive - storeId: {}", id, storeId);
        PurchaseOrderResponse order = purchaseOrderService.receiveOrder(id, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(order, "Purchase order received"));
    }

    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countOrders(@RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long total = purchaseOrderService.countOrders(storeId);
        Long draft = purchaseOrderService.countOrdersByStatus(storeId, "DRAFT");
        Long pending = purchaseOrderService.countOrdersByStatus(storeId, "PENDING");
        Long approved = purchaseOrderService.countOrdersByStatus(storeId, "APPROVED");
        Long received = purchaseOrderService.countOrdersByStatus(storeId, "RECEIVED");

        Map<String, Long> response = new HashMap<>();
        response.put("total", total);
        response.put("draft", draft);
        response.put("pending", pending);
        response.put("approved", approved);
        response.put("received", received);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/date-range")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> getOrdersByDateRange(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/purchase-orders/date-range - storeId: {}, start: {}, end: {}", storeId, startDate, endDate);
        List<PurchaseOrderResponse> orders = purchaseOrderService.getOrdersByDateRange(storeId, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @GetMapping("/{id}/whatsapp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<WhatsAppMessageResponse>> generateWhatsAppMessage(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("GET /api/purchase-orders/{}/whatsapp - storeId: {}", id, storeId);
        WhatsAppMessageResponse response = purchaseOrderService.generateWhatsAppMessage(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/send-whatsapp")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SendWhatsAppResponse>> sendWhatsAppMessage(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("POST /api/purchase-orders/{}/send-whatsapp - storeId: {}", id, storeId);
        SendWhatsAppResponse response = purchaseOrderService.sendWhatsAppMessage(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/send-email")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<SendEmailResponse>> sendPurchaseOrderEmail(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        log.info("POST /api/purchase-orders/{}/send-email - storeId: {}", id, storeId);
        SendEmailResponse response = purchaseOrderService.sendPurchaseOrderEmail(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/total-amount")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTotalAmount(
            @RequestParam Long storeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        storeId = SecurityUtils.getCurrentStoreId();
        BigDecimal total = purchaseOrderService.getTotalPurchasesAmount(storeId, startDate, endDate);
        Map<String, Object> response = new HashMap<>();
        response.put("totalAmount", total != null ? total : BigDecimal.ZERO);
        response.put("startDate", startDate);
        response.put("endDate", endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}