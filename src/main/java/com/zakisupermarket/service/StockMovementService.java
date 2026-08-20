package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.StockMovementRequest;
import com.zakisupermarket.dto.response.StockMovementResponse;
import com.zakisupermarket.dto.response.StockMovementStats;
import com.zakisupermarket.entity.StockMovement;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface StockMovementService {

    StockMovementResponse createMovement(StockMovementRequest request, Long userId, Long storeId);

    Page<StockMovementResponse> getMovementsByStore(Long storeId, int page, int size);
    Page<StockMovementResponse> getMovementsByBatch(Long batchId, Long storeId, int page, int size);
    Page<StockMovementResponse> getMovementsByDateRange(Long storeId, LocalDateTime startDate, LocalDateTime endDate, StockMovement.MovementType type, int page, int size);

    StockMovementStats getMovementStats(Long storeId, LocalDateTime startDate, LocalDateTime endDate);

    void createStockInMovement(Long batchId, Integer quantity, BigDecimal unitPrice, String reference, Long userId);
    void createStockOutMovement(Long batchId, Integer quantity, String reason, String reference, Long userId);
    void createAdjustmentMovement(Long batchId, Integer quantityBefore, Integer quantityAfter, String reason, Long userId);
    void createExpiredMovement(Long batchId, Integer quantity, Long userId);
}