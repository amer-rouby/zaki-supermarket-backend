package com.zakisupermarket.repository;

import com.zakisupermarket.entity.StockAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    Page<StockAlert> findByStoreId(Long storeId, Pageable pageable);

    Page<StockAlert> findByStoreIdAndStatus(Long storeId, String status, Pageable pageable);

    long countByStoreId(Long storeId);

    @Query("""
        SELECT COUNT(sa) FROM StockAlert sa
        WHERE sa.store.id = :storeId
        AND sa.status = 'UNREAD'
    """)
    Long countUnreadAlerts(@Param("storeId") Long storeId);

    @Query("""
        SELECT COUNT(sa) FROM StockAlert sa
        WHERE sa.store.id = :storeId
        AND sa.alertType = :alertType
        AND sa.status != 'RESOLVED'
    """)
    Long countActiveAlertsByType(@Param("storeId") Long storeId,
                                 @Param("alertType") StockAlert.AlertType alertType);

    List<StockAlert> findByStoreIdAndStatusAndCreatedAtAfter(
            Long storeId,
            String status,
            LocalDateTime since
    );

    @Query("""
        SELECT sa FROM StockAlert sa
        WHERE sa.store.id = :storeId
        AND sa.status != 'RESOLVED'
        ORDER BY sa.createdAt DESC
    """)
    List<StockAlert> findActiveAlerts(@Param("storeId") Long storeId);

    // Regardless of status (including RESOLVED) - a resolved alert should not be
    // regenerated the moment the page is reopened, only after the dedup window passes.
    @Query("""
        SELECT COUNT(sa) FROM StockAlert sa
        WHERE sa.store.id = :storeId
        AND sa.alertType = :alertType
        AND (:productId IS NULL OR (sa.product IS NOT NULL AND sa.product.id = :productId))
        AND (:batchId IS NULL OR (sa.batch IS NOT NULL AND sa.batch.id = :batchId))
        AND sa.createdAt > :since
    """)
    long countRecentSimilarAlerts(@Param("storeId") Long storeId,
                                   @Param("alertType") StockAlert.AlertType alertType,
                                   @Param("productId") Long productId,
                                   @Param("batchId") Long batchId,
                                   @Param("since") LocalDateTime since);

    void deleteByCreatedAtBefore(LocalDateTime date);

    // A product that was low/out of stock and got restocked should not keep showing
    // a stale "current stock: 0" alert forever - generateLowStockAlerts only ever
    // created alerts, it never cleared them once the underlying condition resolved.
    @org.springframework.data.jpa.repository.Modifying
    @Query("""
        UPDATE StockAlert sa SET sa.status = 'RESOLVED', sa.resolvedAt = CURRENT_TIMESTAMP
        WHERE sa.store.id = :storeId
        AND sa.product.id = :productId
        AND sa.alertType IN :alertTypes
        AND sa.status != 'RESOLVED'
    """)
    int autoResolveStockAlertsForProduct(@Param("storeId") Long storeId,
                                          @Param("productId") Long productId,
                                          @Param("alertTypes") List<StockAlert.AlertType> alertTypes);
}