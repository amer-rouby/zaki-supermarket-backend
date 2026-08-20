package com.zakisupermarket.repository;

import com.zakisupermarket.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.deletedAt IS NULL")
    List<Product> findByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.deletedAt IS NULL")
    Page<Product> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    @Query("""
        SELECT p FROM Product p WHERE p.store.id = :storeId AND p.deletedAt IS NULL
        AND (:search IS NULL OR :search = '' OR
            LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(p.barcode) LIKE LOWER(CONCAT('%', :search, '%')))
        AND (:category IS NULL OR :category = '' OR p.category = :category)
        """)
    Page<Product> searchAndFilter(@Param("storeId") Long storeId, @Param("search") String search,
                                   @Param("category") String category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND p.store.id = :storeId AND p.deletedAt IS NULL")
    Optional<Product> findByIdAndStoreId(@Param("id") Long id, @Param("storeId") Long storeId);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) AND p.deletedAt IS NULL")
    List<Product> findByStoreIdAndNameContainingIgnoreCase(@Param("storeId") Long storeId, @Param("query") String query);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.barcode = :barcode AND p.deletedAt IS NULL")
    Optional<Product> findByStoreIdAndBarcode(@Param("storeId") Long storeId, @Param("barcode") String barcode);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.store.id = :storeId AND p.barcode = :barcode AND p.deletedAt IS NULL")
    boolean existsByStoreIdAndBarcode(@Param("storeId") Long storeId, @Param("barcode") String barcode);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.store.id = :storeId AND p.deletedAt IS NULL")
    Long countByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT p FROM Product p WHERE p.store.id = :storeId AND p.deletedAt IS NULL")
    List<Product> findActiveProductsByStore(@Param("storeId") Long storeId);

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN p.stockBatches b
        WHERE p.store.id = :storeId
        AND p.deletedAt IS NULL
        GROUP BY p
        HAVING COALESCE(SUM(
            CASE WHEN b.status = 'ACTIVE' THEN b.quantityCurrent ELSE 0 END
        ), 0) <= p.minStockLevel
    """)
    List<Product> findLowStockProducts(@Param("storeId") Long storeId);

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN p.stockBatches b
        WHERE p.store.id = :storeId
        AND p.deletedAt IS NULL
        GROUP BY p
        HAVING COALESCE(SUM(
            CASE WHEN b.status = 'ACTIVE' THEN b.quantityCurrent ELSE 0 END
        ), 0) <= p.minStockLevel
    """)
    Page<Product> findLowStockProducts(@Param("storeId") Long storeId, Pageable pageable);

    @Query("""
        SELECT COUNT(DISTINCT p) FROM Product p
        LEFT JOIN p.stockBatches b
        WHERE p.store.id = :storeId
        AND p.deletedAt IS NULL
        GROUP BY p
        HAVING COALESCE(SUM(
            CASE WHEN b.status = 'ACTIVE' THEN b.quantityCurrent ELSE 0 END
        ), 0) <= p.minStockLevel
    """)
    Long countLowStockProducts(@Param("storeId") Long storeId);

    @Query("""
        SELECT COUNT(DISTINCT p) FROM Product p
        LEFT JOIN p.stockBatches b
        WHERE p.store.id = :storeId
        AND p.deletedAt IS NULL
        GROUP BY p
        HAVING COALESCE(SUM(
            CASE WHEN b.status = 'ACTIVE' THEN b.quantityCurrent ELSE 0 END
        ), 0) = 0
    """)
    Long countOutOfStockProducts(@Param("storeId") Long storeId);

    @Query("""
        SELECT p.id, p.name, p.category, 
               COALESCE(SUM(sb.quantityCurrent), 0),
               p.minStockLevel, p.sellPrice,
               COALESCE(SUM(sb.quantityCurrent * p.sellPrice), 0)
        FROM Product p
        LEFT JOIN StockBatch sb ON p.id = sb.product.id AND sb.status = 'ACTIVE'
        WHERE p.store.id = :storeId
        GROUP BY p.id, p.name, p.category, p.minStockLevel, p.sellPrice
    """)
    List<Object[]> getStockWithCategories(@Param("storeId") Long storeId);

    @Query("""
        SELECT COALESCE(SUM(sb.quantityCurrent * p.sellPrice), 0)
        FROM Product p
        LEFT JOIN StockBatch sb ON p.id = sb.product.id AND sb.status = 'ACTIVE'
        WHERE p.store.id = :storeId
        GROUP BY p
    """)
    BigDecimal getTotalInventoryValue(@Param("storeId") Long storeId);
}