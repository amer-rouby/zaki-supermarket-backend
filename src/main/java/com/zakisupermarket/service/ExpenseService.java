package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.ExpenseRequest;
import com.zakisupermarket.dto.response.ExpenseResponse;
import com.zakisupermarket.dto.response.ExpenseSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface ExpenseService {

    ExpenseResponse createExpense(ExpenseRequest request, Long userId);
    ExpenseResponse updateExpense(Long id, ExpenseRequest request, Long storeId, Long userId);
    void deleteExpense(Long id, Long storeId);
    ExpenseResponse getExpense(Long id, Long storeId);
    Page<ExpenseResponse> getExpenses(Long storeId, Pageable pageable);

    Page<ExpenseResponse> searchExpenses(Long storeId, String query, Pageable pageable);
    Page<ExpenseResponse> getExpensesByCategory(Long storeId, String category, Pageable pageable);

    ExpenseSummaryResponse getExpenseSummary(Long storeId, LocalDateTime startDate, LocalDateTime endDate);
    BigDecimal getTotalExpenses(Long storeId, LocalDateTime startDate, LocalDateTime endDate);
    List<Object[]> getExpensesByCategory(Long storeId, LocalDateTime startDate, LocalDateTime endDate);
}