package com.zakisupermarket.repository;

import com.zakisupermarket.entity.Payment;
import com.zakisupermarket.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByReferenceNumber(String referenceNumber);

    Optional<Payment> findByReferenceNumberAndStoreId(String referenceNumber, Long storeId);

    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);

    Page<Payment> findByStoreId(Long storeId, Pageable pageable);

    Page<Payment> findByStoreIdAndStatus(Long storeId, PaymentStatus status, Pageable pageable);

    List<Payment> findByStoreIdAndStatus(Long storeId, PaymentStatus status);

    @Query("SELECT p FROM Payment p WHERE p.store.id = :storeId " +
            "AND p.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY p.createdAt DESC")
    Page<Payment> findByStoreIdAndDateRange(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    @Query("SELECT COUNT(p) FROM Payment p WHERE p.store.id = :storeId AND p.status = :status")
    Long countByStoreIdAndStatus(@Param("storeId") Long storeId, @Param("status") PaymentStatus status);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.store.id = :storeId AND p.status = 'COMPLETED'")
    BigDecimal getTotalCompletedPayments(@Param("storeId") Long storeId);

    @Query(value = "SELECT * FROM zaki_supermarket.payments p WHERE p.store_id = :storeId " +
            "AND (:status IS NULL OR p.status = :status) " +
            "AND (:paymentMethod IS NULL OR p.payment_method = :paymentMethod) " +
            "AND (:search IS NULL OR CAST(p.reference_number AS TEXT) ILIKE %:search% OR CAST(p.customer_phone AS TEXT) ILIKE %:search%) " +
            "ORDER BY p.created_at DESC", nativeQuery = true)
    Page<Payment> findByStoreIdWithFilters(
            @Param("storeId") Long storeId,
            @Param("status") String status,
            @Param("paymentMethod") String paymentMethod,
            @Param("search") String search,
            Pageable pageable
    );
}