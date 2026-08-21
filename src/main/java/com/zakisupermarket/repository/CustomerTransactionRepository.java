package com.zakisupermarket.repository;

import com.zakisupermarket.entity.CustomerTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerTransactionRepository extends JpaRepository<CustomerTransaction, Long> {

    @Query("""
        SELECT t FROM CustomerTransaction t
        WHERE t.customer.id = :customerId AND t.customer.store.id = :storeId
        ORDER BY t.createdAt DESC
    """)
    List<CustomerTransaction> findByCustomerIdAndStoreIdOrderByCreatedAtDesc(
            @Param("customerId") Long customerId, @Param("storeId") Long storeId);
}
