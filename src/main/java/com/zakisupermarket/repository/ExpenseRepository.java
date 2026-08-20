package com.zakisupermarket.repository;

import com.zakisupermarket.entity.Expense;
import com.zakisupermarket.entity.enums.ExpenseCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByStoreIdAndDeletedAtIsNull(Long storeId, Pageable pageable);

    List<Expense> findByStoreIdAndDeletedAtIsNull(Long storeId);

    Optional<Expense> findByIdAndStoreIdAndDeletedAtIsNull(Long id, Long storeId);

    List<Expense> findByStoreIdAndExpenseDateBetweenAndDeletedAtIsNull(
            Long storeId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    Page<Expense> findByStoreIdAndCategoryAndDeletedAtIsNull(
            Long storeId,
            ExpenseCategory category,
            Pageable pageable);

    Page<Expense> findByStoreIdAndTitleContainingIgnoreCaseAndDeletedAtIsNull(
            Long storeId,
            String title,
            Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
        WHERE e.store.id = :storeId
        AND e.deletedAt IS NULL
    """)
    BigDecimal getTotalExpensesByStore(@Param("storeId") Long storeId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0) FROM Expense e
        WHERE e.store.id = :storeId
        AND e.expenseDate >= :startDate
        AND e.expenseDate <= :endDate
        AND e.deletedAt IS NULL
    """)
    BigDecimal getTotalExpensesByDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT e.category, COALESCE(SUM(e.amount), 0) FROM Expense e
        WHERE e.store.id = :storeId
        AND e.expenseDate >= :startDate
        AND e.expenseDate <= :endDate
        AND e.deletedAt IS NULL
        GROUP BY e.category
    """)
    List<Object[]> getExpensesByCategory(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT FUNCTION('DATE', e.expenseDate), COALESCE(SUM(e.amount), 0), COUNT(e)
        FROM Expense e
        WHERE e.store.id = :storeId
        AND e.expenseDate >= :startDate
        AND e.expenseDate <= :endDate
        AND e.deletedAt IS NULL
        GROUP BY FUNCTION('DATE', e.expenseDate)
        ORDER BY FUNCTION('DATE', e.expenseDate)
    """)
    List<Object[]> getDailyExpenses(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT e FROM Expense e
        WHERE e.store.id = :storeId
        AND e.deletedAt IS NULL
        ORDER BY e.expenseDate DESC
    """)
    List<Expense> findRecentExpenses(@Param("storeId") Long storeId, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Expense e WHERE e.store.id = :storeId AND e.deletedAt IS NULL")
    Long countByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT COUNT(e) FROM Expense e
        WHERE e.store.id = :storeId
        AND e.expenseDate >= :startDate
        AND e.expenseDate <= :endDate
        AND e.deletedAt IS NULL
    """)
    Long countByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}