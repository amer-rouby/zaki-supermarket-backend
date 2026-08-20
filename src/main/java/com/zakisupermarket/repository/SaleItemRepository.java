package com.zakisupermarket.repository;

import com.zakisupermarket.entity.SaleItem;
import com.zakisupermarket.entity.SaleTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SaleItemRepository extends JpaRepository<SaleItem, Long> {

    @Query("SELECT si FROM SaleItem si WHERE si.transaction = :transaction")
    List<SaleItem> findByTransaction(@Param("transaction") SaleTransaction transaction);

    List<SaleItem> findByProductId(Long productId);

    @Query("SELECT si FROM SaleItem si JOIN si.transaction t WHERE t.store.id = :storeId")
    List<SaleItem> findByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT si FROM SaleItem si 
        JOIN si.transaction t 
        WHERE t.store.id = :storeId 
        AND t.transactionDate BETWEEN :startDate AND :endDate
    """)
    List<SaleItem> findByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT si FROM SaleItem si 
        JOIN si.transaction t 
        WHERE t.store.id = :storeId 
        AND CAST(t.transactionDate AS date) BETWEEN :startDate AND :endDate
    """)
    List<SaleItem> findByStoreIdAndDateRangeLocalDate(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT si FROM SaleItem si 
        JOIN si.transaction t 
        WHERE si.product.id = :productId 
        AND t.store.id = :storeId 
        AND t.transactionDate BETWEEN :startDate AND :endDate
    """)
    List<SaleItem> findByProductIdAndStoreIdAndDateBetween(
            @Param("productId") Long productId,
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
        SELECT si FROM SaleItem si 
        JOIN si.transaction t 
        WHERE si.product.id = :productId 
        AND t.store.id = :storeId 
        AND CAST(t.transactionDate AS date) BETWEEN :startDate AND :endDate
    """)
    List<SaleItem> findByProductIdAndStoreIdAndDateBetweenLocalDate(
            @Param("productId") Long productId,
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT SUM(si.quantity) FROM SaleItem si WHERE si.product.id = :productId")
    Long sumQuantityByProductId(@Param("productId") Long productId);

    @Query("SELECT SUM(si.totalPrice) FROM SaleItem si WHERE si.product.id = :productId")
    BigDecimal sumTotalPriceByProductId(@Param("productId") Long productId);

    @Query("""
    SELECT si FROM SaleItem si 
    JOIN si.transaction t 
    JOIN si.product p
    WHERE t.store.id = :storeId 
    AND p.category = :category
    AND t.transactionDate BETWEEN :startDate AND :endDate
    """)
    List<SaleItem> findSalesByCategoryAndDateRange(
            @Param("storeId") Long storeId,
            @Param("category") String category,
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
    SELECT COALESCE(SUM(si.quantity), 0) 
    FROM SaleItem si 
    JOIN si.transaction t 
    WHERE t.store.id = :storeId 
    AND t.transactionDate BETWEEN :startDate AND :endDate
    """)
    Long sumQuantityByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}