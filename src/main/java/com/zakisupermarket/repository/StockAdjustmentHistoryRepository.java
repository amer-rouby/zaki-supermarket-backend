package com.zakisupermarket.repository;

import com.zakisupermarket.entity.StockAdjustmentHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockAdjustmentHistoryRepository extends JpaRepository<StockAdjustmentHistory, Long> {

    List<StockAdjustmentHistory> findByBatchIdOrderByAdjustmentDateDesc(Long batchId);

    Page<StockAdjustmentHistory> findByBatchIdOrderByAdjustmentDateDesc(Long batchId, Pageable pageable);

    @Query("""
        SELECT h FROM StockAdjustmentHistory h
        WHERE h.batch.store.id = :storeId
        ORDER BY h.adjustmentDate DESC
    """)
    Page<StockAdjustmentHistory> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    @Query("""
        SELECT h FROM StockAdjustmentHistory h
        WHERE h.batch.product.id = :productId
        AND h.batch.store.id = :storeId
        ORDER BY h.adjustmentDate DESC
    """)
    List<StockAdjustmentHistory> findByProductIdAndStoreId(
            @Param("productId") Long productId,
            @Param("storeId") Long storeId);
}