package com.zakisupermarket.repository;

import com.zakisupermarket.entity.SaleTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, Long> {

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    Page<SaleTransaction> findByStoreId(Long storeId, Pageable pageable);

    @Query("SELECT st FROM SaleTransaction st WHERE st.id = :id AND st.store.id = :storeId AND st.deletedAt IS NULL")
    Optional<SaleTransaction> findByIdAndStoreId(@Param("id") Long id, @Param("storeId") Long storeId);

    List<SaleTransaction> findByStoreIdAndTransactionDateBetween(
            Long storeId,
            LocalDateTime startDate,
            LocalDateTime endDate);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.user.id = :userId
        AND st.transactionDate > :since
        ORDER BY st.transactionDate DESC
    """)
    List<SaleTransaction> findByStoreIdAndUserIdAndTransactionDateAfter(
            @Param("storeId") Long storeId,
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since);

    // SaleTransaction has @Where(deleted_at IS NULL), which Hibernate applies to
    // every JPQL query against it - a native query is the only way to see
    // soft-deleted rows, which is exactly what "excessive returns" needs to count.
    @Query(value = """
        SELECT st.user_id AS userId, u.full_name AS userName, COUNT(*) AS cnt
        FROM zaki_supermarket.sales_transactions st
        JOIN zaki_supermarket.users u ON u.id = st.user_id
        WHERE st.store_id = :storeId
        AND st.deleted_at IS NOT NULL
        AND st.deleted_at > :since
        GROUP BY st.user_id, u.full_name
    """, nativeQuery = true)
    List<Object[]> countDeletedSalesByUserSince(@Param("storeId") Long storeId, @Param("since") LocalDateTime since);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    Page<SaleTransaction> findByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND (st.invoiceNumber LIKE CONCAT('%', :query, '%') OR st.customerPhone LIKE CONCAT('%', :query, '%'))
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    Page<SaleTransaction> searchSales(
            @Param("storeId") Long storeId,
            @Param("query") String query,
            Pageable pageable);

    @Query("SELECT COUNT(st) FROM SaleTransaction st WHERE st.store.id = :storeId AND st.deletedAt IS NULL")
    Long countByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT COUNT(st) FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
    """)
    Long countByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT COUNT(st) FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND CAST(st.transactionDate AS date) = :date
        AND st.deletedAt IS NULL
    """)
    Long countByStoreIdAndDate(
            @Param("storeId") Long storeId,
            @Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(st.totalAmount), 0) FROM SaleTransaction st WHERE st.store.id = :storeId AND st.deletedAt IS NULL")
    BigDecimal sumTotalAmountByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT COALESCE(SUM(st.totalAmount), 0) FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
    """)
    BigDecimal sumTotalAmountByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT COALESCE(SUM(st.totalAmount), 0) FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND CAST(st.transactionDate AS date) = :date
        AND st.deletedAt IS NULL
    """)
    BigDecimal sumTotalAmountByStoreIdAndDate(
            @Param("storeId") Long storeId,
            @Param("date") LocalDate date);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    List<SaleTransaction> findTop10ByStoreIdOrderByTransactionDateDesc(
            @Param("storeId") Long storeId);

    @Query("""
        SELECT st FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.deletedAt IS NULL
        ORDER BY st.transactionDate DESC
    """)
    List<SaleTransaction> findRecentSalesByStoreId(
            @Param("storeId") Long storeId,
            Pageable pageable);

    @Query("""
        SELECT COALESCE(SUM(st.totalAmount), 0) FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
    """)
    BigDecimal getTotalRevenue(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT COUNT(st) FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
    """)
    Long getTotalOrders(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT st.paymentMethod, COALESCE(SUM(st.totalAmount), 0)
        FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
        GROUP BY st.paymentMethod
    """)
    List<Object[]> getRevenueByPaymentMethod(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT si.product.id, si.product.name, SUM(si.quantity), SUM(si.totalPrice)
        FROM SaleItem si
        JOIN SaleTransaction st ON si.transaction.id = st.id
        WHERE st.store.id = :storeId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
        GROUP BY si.product.id, si.product.name
        ORDER BY SUM(si.quantity) DESC
    """)
    List<Object[]> getTopProducts(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("""
        SELECT CAST(st.transactionDate AS date), COALESCE(SUM(st.totalAmount), 0), COUNT(st)
        FROM SaleTransaction st
        WHERE st.store.id = :storeId
        AND st.transactionDate >= :startDate
        AND st.transactionDate <= :endDate
        AND st.deletedAt IS NULL
        GROUP BY CAST(st.transactionDate AS date)
        ORDER BY CAST(st.transactionDate AS date)
    """)
    List<Object[]> getDailySales(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT si.product.id, si.product.name, SUM(si.quantity), SUM(si.totalPrice)
        FROM SaleItem si
        WHERE si.transaction.store.id = :storeId
        AND si.transaction.deletedAt IS NULL
        GROUP BY si.product.id, si.product.name
        ORDER BY SUM(si.quantity) DESC
    """)
    List<Object[]> findTopSellingProducts(
            @Param("storeId") Long storeId,
            Pageable pageable);

    @Query("""
        SELECT CAST(st.transactionDate AS date) as saleDate, 
               COALESCE(SUM(st.totalAmount), 0) as total
        FROM SaleTransaction st
        WHERE st.store.id = :storeId
          AND st.deletedAt IS NULL
          AND CAST(st.transactionDate AS date) BETWEEN :startDate AND :endDate
        GROUP BY CAST(st.transactionDate AS date)
        ORDER BY saleDate
    """)
    List<Object[]> findDailyRevenueByStoreAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}