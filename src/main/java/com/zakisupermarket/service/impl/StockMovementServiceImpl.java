package com.zakisupermarket.service.impl;

import com.zakisupermarket.dto.request.StockMovementRequest;
import com.zakisupermarket.dto.response.StockMovementResponse;
import com.zakisupermarket.dto.response.StockMovementStats;
import com.zakisupermarket.entity.StockBatch;
import com.zakisupermarket.entity.StockMovement;
import com.zakisupermarket.entity.User;
import com.zakisupermarket.repository.StockBatchRepository;
import com.zakisupermarket.repository.StockMovementRepository;
import com.zakisupermarket.repository.UserRepository;
import com.zakisupermarket.service.StockMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockMovementServiceImpl implements StockMovementService {

    private final StockMovementRepository movementRepository;
    private final StockBatchRepository batchRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public StockMovementResponse createMovement(StockMovementRequest request, Long userId, Long storeId) {
        StockBatch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new RuntimeException("Batch not found: " + request.getBatchId()));

        if (batch.getStore() == null || !batch.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Batch not found: " + request.getBatchId());
        }

        return createMovementInternal(request, userId);
    }

    /**
     * Trusted internal path used by other server-side services (sales, purchase
     * receiving, stock adjustments) that have already resolved and validated the
     * batch/store themselves. Not exposed directly to controllers.
     */
    private StockMovementResponse createMovementInternal(StockMovementRequest request, Long userId) {
        StockBatch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new RuntimeException("Batch not found: " + request.getBatchId()));

        int quantityBefore = batch.getQuantityCurrent();
        int quantityAfter;

        switch (request.getMovementType()) {
            case STOCK_IN, TRANSFER_IN -> quantityAfter = quantityBefore + request.getQuantity();
            case STOCK_OUT, TRANSFER_OUT, EXPIRED, DISCARDED -> {
                if (quantityBefore < request.getQuantity()) {
                    throw new RuntimeException("Insufficient stock. Available: " + quantityBefore);
                }
                quantityAfter = quantityBefore - request.getQuantity();
            }
            case STOCK_ADJUSTMENT -> quantityAfter = request.getQuantity();
            default -> throw new IllegalArgumentException("Invalid movement type: " + request.getMovementType());
        }

        batch.setQuantityCurrent(quantityAfter);
        batchRepository.save(batch);

        BigDecimal totalValue = request.getUnitPrice() != null ?
                request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity())) : null;

        User userRef = null;
        if (userId != null) {
            try {
                userRef = userRepository.getReferenceById(userId);
            } catch (Exception e) {
                log.warn("Could not load user reference for userId: {}. Proceeding without user association.", userId);
            }
        }

        StockMovement movement = StockMovement.builder()
                .batch(batch)
                .movementType(request.getMovementType())
                .quantity(request.getQuantity())
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .unitPrice(request.getUnitPrice())
                .totalValue(totalValue)
                .referenceNumber(request.getReferenceNumber())
                .reason(request.getReason())
                .notes(request.getNotes())
                .user(userRef)
                .storeId(batch.getStore().getId())
                .build();

        movementRepository.save(movement);
        log.info("Stock movement created: type={}, batch={}, qty={} -> {}, user={}",
                request.getMovementType(), batch.getId(), quantityBefore, quantityAfter, userId);

        return StockMovementResponse.fromEntity(movement);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovementsByStore(Long storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return movementRepository.findByStoreIdOrderByMovementDateDesc(storeId, pageable)
                .map(StockMovementResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovementsByBatch(Long batchId, Long storeId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return movementRepository.findByBatchIdAndStoreId(batchId, storeId, pageable)
                .map(StockMovementResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StockMovementResponse> getMovementsByDateRange(Long storeId, LocalDateTime startDate, LocalDateTime endDate, StockMovement.MovementType type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return movementRepository.findByStoreIdAndDateRange(storeId, startDate, endDate, type, pageable)
                .map(StockMovementResponse::fromEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public StockMovementStats getMovementStats(Long storeId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Calculating stats for store: {} from {} to {}", storeId, startDate, endDate);

        Long totalMovements = 0L;
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(storeId, StockMovement.MovementType.STOCK_IN, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(storeId, StockMovement.MovementType.STOCK_OUT, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(storeId, StockMovement.MovementType.STOCK_ADJUSTMENT, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(storeId, StockMovement.MovementType.TRANSFER_IN, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(storeId, StockMovement.MovementType.TRANSFER_OUT, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(storeId, StockMovement.MovementType.EXPIRED, startDate, endDate);
        totalMovements += movementRepository.countMovementsByTypeAndDateRange(storeId, StockMovement.MovementType.DISCARDED, startDate, endDate);

        Integer totalStockIn = movementRepository.sumStockInByDateRange(storeId, startDate, endDate);
        Integer totalStockOut = movementRepository.sumStockOutByDateRange(storeId, startDate, endDate);

        Integer totalAdjustments = movementRepository.sumByTypeAndDateRange(storeId, StockMovement.MovementType.STOCK_ADJUSTMENT, startDate, endDate);
        Integer totalExpired = movementRepository.sumByTypeAndDateRange(storeId, StockMovement.MovementType.EXPIRED, startDate, endDate);
        Integer totalTransferred = movementRepository.sumByTypeAndDateRange(storeId, StockMovement.MovementType.TRANSFER_IN, startDate, endDate);

        StockMovementStats stats = StockMovementStats.builder()
                .totalMovements(totalMovements)
                .totalStockIn(totalStockIn != null ? totalStockIn : 0)
                .totalStockOut(totalStockOut != null ? totalStockOut : 0)
                .totalAdjustments(totalAdjustments != null ? totalAdjustments : 0)
                .totalExpired(totalExpired != null ? totalExpired : 0)
                .totalTransferred(totalTransferred != null ? totalTransferred : 0)
                .build();

        log.info("Stats calculated: totalMovements={}, totalStockIn={}, totalStockOut={}",
                stats.getTotalMovements(), stats.getTotalStockIn(), stats.getTotalStockOut());

        return stats;
    }
    @Override
    @Transactional
    public void createStockInMovement(Long batchId, Integer quantity, BigDecimal unitPrice, String reference, Long userId) {
        StockMovementRequest request = StockMovementRequest.builder()
                .batchId(batchId)
                .movementType(StockMovement.MovementType.STOCK_IN)
                .quantity(quantity)
                .unitPrice(unitPrice)
                .referenceNumber(reference)
                .build();
        createMovementInternal(request, userId);
    }

    @Override
    @Transactional
    public void createStockOutMovement(Long batchId, Integer quantity, String reason, String reference, Long userId) {
        StockMovementRequest request = StockMovementRequest.builder()
                .batchId(batchId)
                .movementType(StockMovement.MovementType.STOCK_OUT)
                .quantity(quantity)
                .reason(reason)
                .referenceNumber(reference)
                .build();
        createMovementInternal(request, userId);
    }

    @Override
    @Transactional
    public void createAdjustmentMovement(Long batchId, Integer quantityBefore, Integer quantityAfter, String reason, Long userId) {
        StockMovementRequest request = StockMovementRequest.builder()
                .batchId(batchId)
                .movementType(StockMovement.MovementType.STOCK_ADJUSTMENT)
                .quantity(quantityAfter)
                .reason(reason)
                .build();
        createMovementInternal(request, userId);
    }

    @Override
    @Transactional
    public void createExpiredMovement(Long batchId, Integer quantity, Long userId) {
        StockMovementRequest request = StockMovementRequest.builder()
                .batchId(batchId)
                .movementType(StockMovement.MovementType.EXPIRED)
                .quantity(quantity)
                .reason("Product expired")
                .build();
        createMovementInternal(request, userId);
    }
}