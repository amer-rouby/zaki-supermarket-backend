package com.zakisupermarket.repository;

import com.zakisupermarket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    boolean existsByStoreIdAndUsername(@Param("storeId") Long storeId, @Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.store.id = :storeId AND u.deletedAt IS NULL ORDER BY u.fullName")
    List<User> findByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT u FROM User u WHERE u.id = :id AND u.store.id = :storeId AND u.deletedAt IS NULL")
    Optional<User> findByIdAndStoreId(@Param("id") Long id, @Param("storeId") Long storeId);

    @Query("SELECT u FROM User u WHERE u.store.id = :storeId AND LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%')) AND u.deletedAt IS NULL")
    List<User> searchByStoreIdAndUsername(@Param("storeId") Long storeId, @Param("query") String query);

    @Query("SELECT COUNT(u) FROM User u WHERE u.store.id = :storeId AND u.deletedAt IS NULL")
    Long countByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT u FROM User u WHERE u.store.id = :storeId AND u.isActive = true AND u.deletedAt IS NULL")
    List<User> findActiveByStoreId(@Param("storeId") Long storeId);

    @Query("SELECT u FROM User u WHERE u.store.id = :storeId AND u.deletedAt IS NULL")
    List<User> findByStoreIdAndIsActiveTrue(@Param("storeId") Long storeId);

    @Modifying @Transactional
    @Query("UPDATE User u SET u.profileImageUrl = :imageUrl WHERE u.id = :userId")
    void updateProfileImageUrl(@Param("userId") Long userId, @Param("imageUrl") String imageUrl);
}