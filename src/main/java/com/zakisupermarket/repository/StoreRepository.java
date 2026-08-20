package com.zakisupermarket.repository;

import com.zakisupermarket.entity.Store;
import com.zakisupermarket.entity.Store.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findByLicenseNumber(String licenseNumber);
    Optional<Store> findByEmail(String email);
    Optional<Store> findByIdAndDeletedAtIsNull(Long id);
    List<Store> findBySubscriptionStatus(SubscriptionStatus status);

    @Query("SELECT p FROM Store p WHERE p.deletedAt IS NULL")
    List<Store> findByDeletedAtIsNull();

    @Query("SELECT p FROM Store p WHERE p.subscriptionStatus = 'ACTIVE' AND p.deletedAt IS NULL")
    List<Store> findActiveStores();

    @Query("SELECT p FROM Store p WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Store> findByIdAndActive(@Param("id") Long id);

    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByEmail(String email);

    @Query("SELECT COUNT(p) FROM Store p WHERE p.deletedAt IS NULL")
    Long countActiveStores();

    @Query("SELECT COUNT(p) FROM Store p WHERE p.subscriptionStatus = :status AND p.deletedAt IS NULL")
    Long countBySubscriptionStatus(@Param("status") SubscriptionStatus status);
}