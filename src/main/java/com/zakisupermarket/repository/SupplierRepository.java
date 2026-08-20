package com.zakisupermarket.repository;

import com.zakisupermarket.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    @Query("""
        SELECT s FROM Supplier s 
        WHERE s.store.id = :storeId 
        AND s.deletedAt IS NULL 
        ORDER BY s.name ASC
    """)
    List<Supplier> findByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT s FROM Supplier s 
        WHERE s.store.id = :storeId 
        AND s.deletedAt IS NULL
    """)
    Page<Supplier> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    Optional<Supplier> findByIdAndStoreIdAndDeletedAtIsNull(Long id, Long storeId);

    @Query("""
        SELECT COUNT(s) FROM Supplier s 
        WHERE s.store.id = :storeId 
        AND s.deletedAt IS NULL
    """)
    Long countByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT s FROM Supplier s 
        WHERE s.store.id = :storeId 
        AND s.deletedAt IS NULL
        AND (s.name LIKE %:query% OR s.contactPerson LIKE %:query% OR s.phone LIKE %:query%)
    """)
    List<Supplier> searchByStoreId(@Param("storeId") Long storeId, @Param("query") String query);

    boolean existsByStoreIdAndNameAndDeletedAtIsNull(Long storeId, String name);
}