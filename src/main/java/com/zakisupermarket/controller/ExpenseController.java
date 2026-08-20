package com.zakisupermarket.controller;

import com.zakisupermarket.dto.request.ExpenseRequest;
import com.zakisupermarket.dto.response.ApiResponse;
import com.zakisupermarket.dto.response.ExpenseResponse;
import com.zakisupermarket.dto.response.ExpenseSummaryResponse;
import com.zakisupermarket.service.ExpenseService;
import com.zakisupermarket.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/expenses")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class ExpenseController {

    private final ExpenseService expenseService;

    @PostMapping
    public ResponseEntity<ApiResponse<ExpenseResponse>> createExpense(
            @RequestBody @Valid ExpenseRequest request) {

        request.setStoreId(SecurityUtils.getCurrentStoreId());
        Long effectiveUserId = SecurityUtils.getCurrentUserId();

        ExpenseResponse response = expenseService.createExpense(request, effectiveUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Expense created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpenses(
            @RequestParam Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "expenseDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        storeId = SecurityUtils.getCurrentStoreId();

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Page<ExpenseResponse> expenses = expenseService.getExpenses(
                storeId, PageRequest.of(page, size, sort));

        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> getExpense(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        ExpenseResponse response = expenseService.getExpense(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ExpenseResponse>> updateExpense(
            @PathVariable Long id,
            @RequestBody @Valid ExpenseRequest request,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        Long userId = SecurityUtils.getCurrentUserId();
        ExpenseResponse response = expenseService.updateExpense(id, request, storeId, userId);
        return ResponseEntity.ok(ApiResponse.success(response, "Expense updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable Long id,
            @RequestParam Long storeId) {
        storeId = SecurityUtils.getCurrentStoreId();
        expenseService.deleteExpense(id, storeId);
        return ResponseEntity.ok(ApiResponse.success(null, "Expense deleted successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> searchExpenses(
            @RequestParam Long storeId,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        storeId = SecurityUtils.getCurrentStoreId();

        Page<ExpenseResponse> results = expenseService.searchExpenses(
                storeId, query, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<Page<ExpenseResponse>>> getExpensesByCategory(
            @RequestParam Long storeId,
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        storeId = SecurityUtils.getCurrentStoreId();

        Page<ExpenseResponse> results = expenseService.getExpensesByCategory(
                storeId, category, PageRequest.of(page, size));

        return ResponseEntity.ok(ApiResponse.success(results));
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<ExpenseSummaryResponse>> getExpenseSummary(
            @RequestParam Long storeId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        storeId = SecurityUtils.getCurrentStoreId();

        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : LocalDate.now().minusDays(30).atStartOfDay();
        LocalDateTime end = endDate != null ? endDate.atTime(23, 59, 59) : LocalDateTime.now();

        ExpenseSummaryResponse summary = expenseService.getExpenseSummary(storeId, start, end);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }
}