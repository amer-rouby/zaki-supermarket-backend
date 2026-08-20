package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.ReportRequest;
import com.zakisupermarket.dto.response.*;

public interface ReportService {
    SalesReportResponse getSalesReport(ReportRequest request);
    StockReportResponse getStockReport(ReportRequest request);
    FinancialReportResponse getFinancialReport(ReportRequest request);
    ExpiryReportResponse getExpiryReport(ReportRequest request);
}