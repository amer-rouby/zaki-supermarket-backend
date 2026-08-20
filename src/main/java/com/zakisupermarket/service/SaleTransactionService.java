package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.SaleRequest;
import com.zakisupermarket.dto.response.SaleTransactionDTO;
import com.zakisupermarket.dto.response.SalesReportResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface SaleTransactionService {

    Page<SaleTransactionDTO> getAllSales(Long storeId, int page, int size);
    Page<SaleTransactionDTO> getAllSales(Long storeId, int page, int size,
                                         String sortBy, String sortDirection);
    SaleTransactionDTO getSaleById(Long id, Long storeId);
    SaleTransactionDTO createSale(SaleRequest request, Long currentUserId);
    SaleTransactionDTO updateSale(Long id, SaleRequest request, Long storeId);
    void deleteSale(Long id, Long storeId);
    SalesReportResponse getSalesAnalytics(Long storeId, LocalDate startDate,
                                          LocalDate endDate, String period);
    Map<String, Object> getSalesStats(Long storeId);
    Map<String, Object> getTodaySales(Long storeId);
    Page<SaleTransactionDTO> getSalesByDateRange(Long storeId,
                                                 LocalDate startDate,
                                                 LocalDate endDate);
    Page<SaleTransactionDTO> searchSales(Long storeId, String query);
    List<SaleTransactionDTO> getRecentSales(Long storeId, int limit);
    Map<String, Object> getSalesByCategory(Long storeId,
                                           LocalDate startDate,
                                           LocalDate endDate);
    List<Map<String, Object>> getTopProducts(Long storeId, int limit);
}