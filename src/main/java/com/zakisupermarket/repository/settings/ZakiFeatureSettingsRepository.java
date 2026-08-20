package com.zakisupermarket.repository.settings;

import com.zakisupermarket.entity.settings.ZakiFeatureSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ZakiFeatureSettingsRepository extends JpaRepository<ZakiFeatureSettings, Long> {

    @Query("SELECT s FROM ZakiFeatureSettings s WHERE s.store.id = :storeId")
    Optional<ZakiFeatureSettings> findByStoreId(@Param("storeId") Long storeId);
}
