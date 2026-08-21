package com.zakisupermarket.repository;

import com.zakisupermarket.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("SELECT c FROM Customer c WHERE c.store.id = :storeId AND c.deletedAt IS NULL ORDER BY c.name ASC")
    List<Customer> findByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT c FROM Customer c WHERE c.store.id = :storeId AND c.deletedAt IS NULL ORDER BY c.name ASC")
    Page<Customer> findByStoreId(@Param("storeId") Long storeId, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE c.id = :id AND c.store.id = :storeId AND c.deletedAt IS NULL")
    Optional<Customer> findByIdAndStoreIdAndDeletedAtIsNull(@Param("id") Long id, @Param("storeId") Long storeId);

    @Query("""
        SELECT c FROM Customer c
        WHERE c.store.id = :storeId AND c.deletedAt IS NULL
        AND (LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%')) OR c.phone LIKE CONCAT('%', :query, '%'))
        ORDER BY c.name ASC
    """)
    List<Customer> searchByStoreId(@Param("storeId") Long storeId, @Param("query") String query);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.store.id = :storeId AND c.deletedAt IS NULL")
    Long countByStoreId(@Param("storeId") Long storeId);

    boolean existsByStoreIdAndPhoneAndDeletedAtIsNull(Long storeId, String phone);
}
