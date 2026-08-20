package com.zakisupermarket.repository;

import com.zakisupermarket.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.store.id = :storeId AND c.deletedAt IS NULL ORDER BY c.nameAr, c.name")
    List<Category> findByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.store.id = :storeId AND c.deletedAt IS NULL")
    Optional<Category> findByIdAndStoreId(@Param("id") Long id, @Param("storeId") Long storeId);

    @Query("""
            SELECT c FROM Category c
            WHERE c.store.id = :storeId
            AND (
                LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.nameAr) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :query, '%'))
            )
            AND c.deletedAt IS NULL
            """)
    List<Category> searchByStoreIdAndName(@Param("storeId") Long storeId, @Param("query") String query);

    @Query("""
            SELECT COUNT(c) > 0 FROM Category c
            WHERE c.store.id = :storeId
            AND c.deletedAt IS NULL
            AND (
                LOWER(c.name) = LOWER(:name)
                OR LOWER(c.nameAr) = LOWER(:name)
                OR LOWER(c.nameEn) = LOWER(:name)
            )
            """)
    boolean existsByStoreIdAndNameIgnoreCase(@Param("storeId") Long storeId, @Param("name") String name);

    @Query("SELECT COUNT(c) FROM Category c WHERE c.store.id = :storeId AND c.deletedAt IS NULL")
    Long countByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT c FROM Category c WHERE c.store.id = :storeId AND c.isActive = true AND c.deletedAt IS NULL")
    List<Category> findActiveByStoreId(@Param("storeId") Long storeId);

    @Query("""
            SELECT c FROM Category c
            WHERE c.store.id = :storeId
            AND c.deletedAt IS NULL
            AND (:search IS NULL OR :search = '' OR
                LOWER(c.name) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.nameAr) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.nameEn) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Category> searchAndFilter(@Param("storeId") Long storeId, @Param("search") String search, Pageable pageable);
}
