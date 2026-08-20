package com.zakisupermarket.repository;

import com.zakisupermarket.entity.StockBatch;
import com.zakisupermarket.entity.StockBatch.BatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    List<StockBatch> findByStoreId(Long storeId);

    Page<StockBatch> findByStoreIdAndStatus(Long storeId, BatchStatus status, Pageable pageable);

    List<StockBatch> findByStoreIdAndStatus(Long storeId, BatchStatus status);

    List<StockBatch> findByProductIdAndStatus(Long productId, BatchStatus status);

    default List<StockBatch> findByProductIdAndStatusActive(Long productId) {
        return findByProductIdAndStatus(productId, BatchStatus.ACTIVE);
    }

    @Query("""
        SELECT sb FROM StockBatch sb 
        WHERE sb.product.id = :productId 
        AND sb.status = 'ACTIVE' 
        AND sb.quantityCurrent > 0
        ORDER BY sb.expiryDate ASC
    """)
    List<StockBatch> findActiveBatchesByProductOrderedByExpiry(@Param("productId") Long productId);

    @Query("""
        SELECT sb FROM StockBatch sb 
        WHERE sb.store.id = :storeId 
        AND sb.status = 'ACTIVE' 
        AND sb.expiryDate <= :expiryDate
        ORDER BY sb.expiryDate ASC
    """)
    List<StockBatch> findExpiringBatches(@Param("storeId") Long storeId, @Param("expiryDate") LocalDate expiryDate);

    @Query("""
    SELECT sb FROM StockBatch sb 
    WHERE sb.store.id = :storeId 
    AND sb.status = 'ACTIVE' 
    AND sb.expiryDate < :expiryDate
    ORDER BY sb.expiryDate ASC
""")
    List<StockBatch> findExpiredBatches(
            @Param("storeId") Long storeId,
            @Param("expiryDate") LocalDate expiryDate
    );
    @Lock(jakarta.persistence.LockModeType.OPTIMISTIC)
    StockBatch findByIdAndStatus(Long id, BatchStatus status);

    @Query("""
        SELECT COALESCE(SUM(sb.quantityCurrent), 0) 
        FROM StockBatch sb 
        WHERE sb.product.id = :productId 
        AND sb.status = 'ACTIVE'
    """)
    Long sumQuantityByProductId(@Param("productId") Long productId);

    // Bulk variant of the above for callers that need every product's total in one
    // pass (e.g. low-stock alert generation) - avoids issuing one query per product.
    @Query("""
        SELECT sb.product.id, COALESCE(SUM(sb.quantityCurrent), 0)
        FROM StockBatch sb
        WHERE sb.product.store.id = :storeId
        AND sb.status = 'ACTIVE'
        GROUP BY sb.product.id
    """)
    List<Object[]> sumQuantityGroupedByProductForStore(@Param("storeId") Long storeId);

    @Query("""
        UPDATE StockBatch sb 
        SET sb.quantityCurrent = sb.quantityCurrent - :deduct 
        WHERE sb.id = :batchId 
        AND sb.quantityCurrent >= :deduct
    """)
    int deductQuantity(@Param("batchId") Long batchId, @Param("deduct") Integer deduct);

    @Query("""
        SELECT COUNT(sb) FROM StockBatch sb
        WHERE sb.store.id = :storeId
        AND sb.status = 'ACTIVE'
        AND sb.expiryDate BETWEEN CURRENT_DATE AND :expiryDate
    """)
    Long countExpiringBatches(@Param("storeId") Long storeId, @Param("expiryDate") LocalDate expiryDate);

    @Query("""
        SELECT COUNT(sb) FROM StockBatch sb
        WHERE sb.store.id = :storeId
        AND sb.status = 'ACTIVE'
        AND sb.expiryDate < CURRENT_DATE
    """)
    Long countExpiredBatches(@Param("storeId") Long storeId);

    @Query("""
        SELECT COALESCE(SUM(sb.quantityCurrent * p.sellPrice), 0)
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.store.id = :storeId
        AND sb.status = 'ACTIVE'
    """)
    BigDecimal getTotalStockValue(@Param("storeId") Long storeId);

    @Query("""
        SELECT p.category, COUNT(DISTINCT p.id), COALESCE(SUM(sb.quantityCurrent * p.sellPrice), 0)
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.store.id = :storeId
        AND sb.status = 'ACTIVE'
        GROUP BY p.category
    """)
    List<Object[]> getStockByCategory(@Param("storeId") Long storeId);

    @Query("""
        SELECT p.id, p.name, sb.batchNumber, SUM(sb.quantityCurrent), p.minStockLevel, sb.expiryDate
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.store.id = :storeId
        AND sb.status = 'ACTIVE'
        GROUP BY p.id, p.name, sb.batchNumber, p.minStockLevel, sb.expiryDate
        HAVING SUM(sb.quantityCurrent) <= p.minStockLevel
        ORDER BY SUM(sb.quantityCurrent) ASC
    """)
    List<Object[]> getLowStockProducts(@Param("storeId") Long storeId);

    @Query("""
        SELECT p.id, p.name, sb.batchNumber, sb.expiryDate, SUM(sb.quantityCurrent)
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.store.id = :storeId
        AND sb.status = 'ACTIVE'
        AND sb.expiryDate <= :expiryDate
        GROUP BY p.id, p.name, sb.batchNumber, sb.expiryDate
        ORDER BY sb.expiryDate ASC
    """)
    List<Object[]> getExpiringProducts(@Param("storeId") Long storeId,
                                       @Param("expiryDate") LocalDate expiryDate);

    @Query("""
    SELECT p.id, p.name, sb.batchNumber, sb.expiryDate, SUM(sb.quantityCurrent)
    FROM StockBatch sb
    JOIN Product p ON sb.product.id = p.id
    WHERE sb.store.id = :storeId
    AND sb.status = 'ACTIVE'
    AND sb.expiryDate < :today
    GROUP BY p.id, p.name, sb.batchNumber, sb.expiryDate
    ORDER BY sb.expiryDate ASC
""")
    List<Object[]> getExpiredProducts(@Param("storeId") Long storeId, @Param("today") LocalDate today);
    @Query("""
        SELECT COALESCE(SUM(sb.quantityCurrent), 0)
        FROM StockBatch sb
        WHERE sb.store.id = :storeId
        AND sb.status = 'ACTIVE'
    """)
    Long getTotalStockItemsCount(@Param("storeId") Long storeId);

    @Query("""
        SELECT COUNT(DISTINCT p.id)
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.store.id = :storeId
        AND sb.status = 'ACTIVE'
        GROUP BY p.id, p.minStockLevel
        HAVING SUM(sb.quantityCurrent) <= p.minStockLevel
    """)
    Long countLowStockItems(@Param("storeId") Long storeId);

    @Query("""
        SELECT p.id, p.name, sb.batchNumber, sb.expiryDate, sb.quantityCurrent
        FROM StockBatch sb
        JOIN Product p ON sb.product.id = p.id
        WHERE sb.store.id = :storeId
        AND sb.status = 'ACTIVE'
        AND sb.expiryDate >= :startDate
        AND sb.expiryDate <= :endDate
    """)
    List<Object[]> getExpiringBatchesInRange(@Param("storeId") Long storeId,
                                             @Param("startDate") LocalDate startDate,
                                             @Param("endDate") LocalDate endDate);
}