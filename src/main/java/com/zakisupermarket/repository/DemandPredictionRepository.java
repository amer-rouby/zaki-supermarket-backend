package com.zakisupermarket.repository;

import com.zakisupermarket.entity.DemandPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DemandPredictionRepository extends JpaRepository<DemandPrediction, Long> {

    @Query("""
        SELECT dp FROM DemandPrediction dp
        WHERE dp.store.id = :storeId
        AND dp.predictionDate >= :startDate
        AND dp.predictionDate <= :endDate
        AND dp.actualQuantity IS NULL
        ORDER BY dp.predictionDate ASC
    """)
    List<DemandPrediction> findUpcomingPredictions(
            @Param("storeId") Long storeId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    Page<DemandPrediction> findByStoreId(Long storeId, Pageable pageable);

    Page<DemandPrediction> findByStoreIdAndPredictionDateGreaterThanEqualOrderByPredictionDateAsc(
            Long storeId, LocalDate fromDate, Pageable pageable
    );

    List<DemandPrediction> findByPredictionDateBeforeAndActualQuantityIsNull(LocalDate date);

    Optional<DemandPrediction> findByProductIdAndStoreIdAndPredictionDate(
            Long productId, Long storeId, LocalDate predictionDate
    );

    @Query("SELECT COUNT(dp) FROM DemandPrediction dp WHERE dp.store.id = :storeId")
    Long countByStoreId(@Param("storeId") Long storeId);

    @Query("""
        SELECT AVG(dp.accuracyPercentage) FROM DemandPrediction dp
        WHERE dp.store.id = :storeId
        AND dp.actualQuantity IS NOT NULL
        AND dp.accuracyPercentage IS NOT NULL
    """)
    BigDecimal calculateAverageAccuracyByStore(@Param("storeId") Long storeId);

    @Query("SELECT MAX(dp.predictionDate) FROM DemandPrediction dp WHERE dp.store.id = :storeId")
    Optional<LocalDate> findLatestPredictionDateByStore(@Param("storeId") Long storeId);

    void deleteByStoreIdAndPredictionDateBefore(Long storeId, LocalDate date);
}