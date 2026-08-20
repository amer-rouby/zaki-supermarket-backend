package com.zakisupermarket.repository.settings;

import com.zakisupermarket.entity.settings.StoreSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreSettingsRepository extends JpaRepository<StoreSettings, Long> {

    @Query("SELECT s FROM StoreSettings s WHERE s.store.id = :storeId")
    Optional<StoreSettings> findByStoreId(@Param("storeId") Long storeId);

    boolean existsByStoreId(@Param("storeId") Long storeId);
}