package com.zakisupermarket.repository;

import com.zakisupermarket.entity.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {

    @Query("""
        SELECT po FROM PurchaseOrder po 
        WHERE po.store.id = :storeId 
        AND po.deletedAt IS NULL 
        ORDER BY po.createdAt DESC
    """)
    Page<PurchaseOrder> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    @Query("""
        SELECT po FROM PurchaseOrder po 
        WHERE po.store.id = :storeId 
        AND po.deletedAt IS NULL
        AND po.status = :status
        ORDER BY po.createdAt DESC
    """)
    Page<PurchaseOrder> findByStoreIdAndStatus(@Param("storeId") Long storeId,
                                                  @Param("status") String status,
                                                  Pageable pageable);

    Optional<PurchaseOrder> findByIdAndStoreIdAndDeletedAtIsNull(Long id, Long storeId);

    @Query("""
        SELECT COUNT(po) FROM PurchaseOrder po 
        WHERE po.store.id = :storeId 
        AND po.deletedAt IS NULL
    """)
    Long countByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT COUNT(po) FROM PurchaseOrder po 
        WHERE po.store.id = :storeId 
        AND po.deletedAt IS NULL 
        AND po.status = :status
    """)
    Long countByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") String status);

    @Query("""
        SELECT po FROM PurchaseOrder po 
        WHERE po.store.id = :storeId 
        AND po.deletedAt IS NULL
        AND po.orderDate BETWEEN :startDate AND :endDate
        ORDER BY po.orderDate DESC
    """)
    List<PurchaseOrder> findByStoreIdAndDateRange(@Param("storeId") Long storeId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    @Query("""
        SELECT SUM(po.totalAmount) FROM PurchaseOrder po 
        WHERE po.store.id = :storeId 
        AND po.deletedAt IS NULL 
        AND po.status = 'RECEIVED'
        AND po.orderDate BETWEEN :startDate AND :endDate
    """)
    java.math.BigDecimal sumTotalAmountByStoreIdAndDateRange(@Param("storeId") Long storeId,
                                                                @Param("startDate") LocalDate startDate,
                                                                @Param("endDate") LocalDate endDate);

    boolean existsByStoreIdAndOrderNumberAndDeletedAtIsNull(Long storeId, String orderNumber);

    @Query("""
        SELECT po FROM PurchaseOrder po 
        WHERE po.sourceType = 'PREDICTION' 
        AND po.sourceId = :predictionId 
        AND po.deletedAt IS NULL
    """)
    List<PurchaseOrder> findByPredictionId(@Param("predictionId") Long predictionId);
}