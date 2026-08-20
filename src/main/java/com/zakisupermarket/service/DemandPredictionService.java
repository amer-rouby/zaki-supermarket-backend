package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.UpdatePredictionDTO;
import com.zakisupermarket.dto.response.DemandPredictionResponse;
import com.zakisupermarket.dto.response.ReorderRecommendationDTO;
import com.zakisupermarket.dto.response.SalesHistoryPointDTO;
import com.zakisupermarket.dto.response.SupplierReorderGroupDTO;
import com.zakisupermarket.dto.response.ShareLinkResponse;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface DemandPredictionService {

    void generatePredictions(Long storeId, LocalDate forDate);

    void generateWeeklyPredictionsForAllStores();

    void updatePastPredictionsWithActuals();

    DemandPredictionResponse generatePredictionForProduct(Long productId, Long storeId, LocalDate forDate);

    List<DemandPredictionResponse> getUpcomingPredictions(Long storeId, int daysAhead);

    Page<DemandPredictionResponse> getPredictions(Long storeId, int page, int size);

    DemandPredictionResponse getPredictionById(Long predictionId, Long storeId);

    void updatePredictionWithActual(Long predictionId, Integer actualQuantity, Long storeId);

    Map<String, Object> getAccuracyStats(Long storeId);

    Integer calculateSimpleForecast(List<Integer> historicalSales, LocalDate predictionDate, String productCategory);

    DemandPredictionResponse updatePrediction(Long predictionId, UpdatePredictionDTO updates, Long storeId);

    void deletePrediction(Long predictionId, Long storeId);

    byte[] exportPredictionToPdf(Long predictionId, Long storeId);

    byte[] exportPredictionToExcel(Long predictionId, Long storeId);

    ShareLinkResponse generateShareLink(Long predictionId, Long storeId, Long userId, int expiryHours);

    List<SalesHistoryPointDTO> getProductSalesHistory(Long productId, Long storeId, int days);

    List<ReorderRecommendationDTO> getReorderRecommendations(Long storeId);

    List<SupplierReorderGroupDTO> getReorderRecommendationsBySupplier(Long storeId);
}