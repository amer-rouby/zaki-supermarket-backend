package com.zakisupermarket.repository;

import com.zakisupermarket.entity.PurchaseOrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

    List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);

    @Query("""
        SELECT poi FROM PurchaseOrderItem poi 
        JOIN poi.purchaseOrder po 
        WHERE po.store.id = :storeId 
        AND po.deletedAt IS NULL
    """)
    List<PurchaseOrderItem> findByStoreId(@Param("storeId") Long storeId);

    void deleteByPurchaseOrderId(Long purchaseOrderId);

    @Query("""
        SELECT SUM(poi.quantity) FROM PurchaseOrderItem poi 
        JOIN poi.purchaseOrder po 
        WHERE poi.product.id = :productId 
        AND po.store.id = :storeId 
        AND po.status = 'RECEIVED'
        AND po.deletedAt IS NULL
    """)
    Long sumQuantityByProductIdAndStoreId(@Param("productId") Long productId,
                                             @Param("storeId") Long storeId);

    @Query("""
        SELECT poi FROM PurchaseOrderItem poi
        JOIN poi.purchaseOrder po
        WHERE poi.product.id = :productId
        AND po.store.id = :storeId
        AND po.supplier IS NOT NULL
        AND po.deletedAt IS NULL
        ORDER BY po.orderDate DESC, po.createdAt DESC
    """)
    List<PurchaseOrderItem> findMostRecentByProductIdAndStoreId(@Param("productId") Long productId,
                                                                  @Param("storeId") Long storeId,
                                                                  Pageable pageable);
}