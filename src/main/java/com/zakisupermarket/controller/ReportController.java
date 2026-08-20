package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.ReportRequest;
import com.zakisupermarket.dto.response.*;
import com.zakisupermarket.service.ReportService;
import com.zakisupermarket.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'PHARMACIST')")
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/sales")
    public ResponseEntity<ApiResponse<SalesReportResponse>> getSalesReport(
            @RequestBody ReportRequest request) {
        request.setStoreId(SecurityUtils.getCurrentStoreId());
        SalesReportResponse response = reportService.getSalesReport(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/stock")
    public ResponseEntity<ApiResponse<StockReportResponse>> getStockReport(
            @RequestBody ReportRequest request) {
        request.setStoreId(SecurityUtils.getCurrentStoreId());
        StockReportResponse response = reportService.getStockReport(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/financial")
    public ResponseEntity<ApiResponse<FinancialReportResponse>> getFinancialReport(
            @RequestBody ReportRequest request) {
        request.setStoreId(SecurityUtils.getCurrentStoreId());
        FinancialReportResponse response = reportService.getFinancialReport(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/expiry")
    public ResponseEntity<ApiResponse<ExpiryReportResponse>> getExpiryReport(
            @RequestBody ReportRequest request) {
        request.setStoreId(SecurityUtils.getCurrentStoreId());
        ExpiryReportResponse response = reportService.getExpiryReport(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
