package com.zakisupermarket.repository;

import com.zakisupermarket.entity.AnomalyDetection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AnomalyDetectionRepository extends JpaRepository<AnomalyDetection, Long> {

    Page<AnomalyDetection> findByStoreIdOrderByDetectedAtDesc(Long storeId, Pageable pageable);

    Page<AnomalyDetection> findByStoreIdAndStatusOrderByDetectedAtDesc(Long storeId, AnomalyDetection.Status status, Pageable pageable);

    Page<AnomalyDetection> findByStoreIdAndTypeOrderByDetectedAtDesc(Long storeId, AnomalyDetection.Type type, Pageable pageable);

    Page<AnomalyDetection> findByStoreIdAndStatusAndTypeOrderByDetectedAtDesc(
            Long storeId, AnomalyDetection.Status status, AnomalyDetection.Type type, Pageable pageable);

    long countByStoreIdAndStatus(Long storeId, AnomalyDetection.Status status);

    @Query("""
        SELECT a FROM AnomalyDetection a
        WHERE a.store.id = :storeId
        AND a.type = :type
        AND a.relatedEntityType = :relatedEntityType
        AND a.relatedEntityId = :relatedEntityId
        AND a.detectedAt > :since
    """)
    List<AnomalyDetection> findRecentDuplicates(
            @Param("storeId") Long storeId,
            @Param("type") AnomalyDetection.Type type,
            @Param("relatedEntityType") String relatedEntityType,
            @Param("relatedEntityId") Long relatedEntityId,
            @Param("since") LocalDateTime since);

    Optional<AnomalyDetection> findByIdAndStoreId(Long id, Long storeId);
}
