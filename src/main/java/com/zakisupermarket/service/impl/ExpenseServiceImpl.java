package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.ExpenseRequest;
import com.zakisupermarket.dto.response.ExpenseResponse;
import com.zakisupermarket.dto.response.ExpenseSummaryResponse;
import com.zakisupermarket.entity.Expense;
import com.zakisupermarket.entity.enums.ExpenseCategory;
import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.User;
import com.zakisupermarket.exception.ResourceNotFoundException;
import com.zakisupermarket.repository.ExpenseRepository;
import com.zakisupermarket.repository.StoreRepository;
import com.zakisupermarket.repository.UserRepository;
import com.zakisupermarket.service.ExpenseService;
import com.zakisupermarket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ExpenseServiceImpl implements ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Override
    public ExpenseResponse createExpense(ExpenseRequest request, Long userId) {
        Store store = storeRepository.findByIdAndDeletedAtIsNull(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Expense expense = Expense.builder()
                .store(store)
                .category(request.getCategory())
                .title(request.getTitle())
                .description(request.getDescription())
                .amount(request.getAmount())
                .expenseDate(request.getExpenseDate())
                .paymentMethod(request.getPaymentMethod())
                .referenceNumber(request.getReferenceNumber())
                .attachmentUrl(request.getAttachmentUrl())
                .createdBy(user)
                .build();

        Expense saved = expenseRepository.save(expense);
        try {
            notificationService.notifyExpenseAdded(store.getId(), saved.getId(), saved.getAmount());
        } catch (Exception e) {
            log.warn("Failed to create expense notification for expense {}: {}", saved.getId(), e.getMessage());
        }
        return mapToResponse(saved);
    }

    @Override
    public ExpenseResponse updateExpense(Long id, ExpenseRequest request, Long storeId, Long userId) {
        Expense expense = expenseRepository.findByIdAndStoreIdAndDeletedAtIsNull(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        expense.setCategory(request.getCategory());
        expense.setTitle(request.getTitle());
        expense.setDescription(request.getDescription());
        expense.setAmount(request.getAmount());
        expense.setExpenseDate(request.getExpenseDate());
        expense.setPaymentMethod(request.getPaymentMethod());
        expense.setReferenceNumber(request.getReferenceNumber());
        expense.setAttachmentUrl(request.getAttachmentUrl());

        Expense updated = expenseRepository.save(expense);
        return mapToResponse(updated);
    }

    @Override
    public void deleteExpense(Long id, Long storeId) {
        Expense expense = expenseRepository.findByIdAndStoreIdAndDeletedAtIsNull(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));

        expense.markAsDeleted();
        expenseRepository.save(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseResponse getExpense(Long id, Long storeId) {
        Expense expense = expenseRepository.findByIdAndStoreIdAndDeletedAtIsNull(id, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Expense not found"));
        return mapToResponse(expense);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpenses(Long storeId, Pageable pageable) {
        return expenseRepository.findByStoreIdAndDeletedAtIsNull(storeId, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> searchExpenses(Long storeId, String query, Pageable pageable) {
        return expenseRepository.findByStoreIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
                storeId, query, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExpenseResponse> getExpensesByCategory(Long storeId, String category, Pageable pageable) {
        ExpenseCategory expenseCategory = ExpenseCategory.valueOf(category.toUpperCase());
        return expenseRepository.findByStoreIdAndCategoryAndDeletedAtIsNull(
                storeId, expenseCategory, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpenseSummaryResponse getExpenseSummary(Long storeId, LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal totalExpenses = expenseRepository.getTotalExpensesByDateRange(storeId, startDate, endDate);
        Long totalTransactions = expenseRepository.countByStoreIdAndDateRange(storeId, startDate, endDate);
        BigDecimal averageExpense = totalTransactions > 0
                ? totalExpenses.divide(BigDecimal.valueOf(totalTransactions), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO;

        List<Object[]> categoryData = expenseRepository.getExpensesByCategory(storeId, startDate, endDate);
        Map<String, BigDecimal> expensesByCategory = categoryData.stream()
                .collect(Collectors.toMap(
                        row -> ((ExpenseCategory) row[0]).getArabicName(),
                        row -> (BigDecimal) row[1]
                ));

        List<Object[]> dailyData = expenseRepository.getDailyExpenses(storeId, startDate, endDate);
        List<ExpenseSummaryResponse.DailyExpenseDTO> dailyExpenses = dailyData.stream()
                .map(row -> ExpenseSummaryResponse.DailyExpenseDTO.builder()
                        .date(row[0].toString())
                        .amount((BigDecimal) row[1])
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        List<Expense> recent = expenseRepository.findRecentExpenses(storeId, Pageable.ofSize(5));
        List<ExpenseSummaryResponse.RecentExpenseDTO> recentExpenses = recent.stream()
                .map(e -> ExpenseSummaryResponse.RecentExpenseDTO.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .category(e.getCategory())
                        .amount(e.getAmount())
                        .expenseDate(e.getExpenseDate())
                        .build())
                .collect(Collectors.toList());

        return ExpenseSummaryResponse.builder()
                .totalExpenses(totalExpenses)
                .totalTransactions(totalTransactions)
                .averageExpense(averageExpense)
                .expensesByCategory(expensesByCategory)
                .dailyExpenses(dailyExpenses)
                .recentExpenses(recentExpenses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalExpenses(Long storeId, LocalDateTime startDate, LocalDateTime endDate) {
        return expenseRepository.getTotalExpensesByDateRange(storeId, startDate, endDate);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Object[]> getExpensesByCategory(Long storeId, LocalDateTime startDate, LocalDateTime endDate) {
        return expenseRepository.getExpensesByCategory(storeId, startDate, endDate);
    }

    private ExpenseResponse mapToResponse(Expense expense) {
        return ExpenseResponse.builder()
                .id(expense.getId())
                .storeId(expense.getStore().getId())
                .category(expense.getCategory())
                .categoryArabic(expense.getCategory().getArabicName())
                .title(expense.getTitle())
                .description(expense.getDescription())
                .amount(expense.getAmount())
                .expenseDate(expense.getExpenseDate())
                .paymentMethod(expense.getPaymentMethod())
                .referenceNumber(expense.getReferenceNumber())
                .attachmentUrl(expense.getAttachmentUrl())
                .createdBy(expense.getCreatedBy() != null ? expense.getCreatedBy().getFullName() : null)
                .createdAt(expense.getCreatedAt())
                .updatedAt(expense.getUpdatedAt())
                .build();
    }
}