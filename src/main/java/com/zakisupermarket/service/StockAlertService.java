package com.zakisupermarket.service;

import com.zakisupermarket.dto.response.AlertStatsResponse;
import com.zakisupermarket.dto.response.StockAlertResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface StockAlertService {

    Page<StockAlertResponse> getAlerts(Long storeId, int page, int size);

    Page<StockAlertResponse> getAlertsByStatus(Long storeId, String status, int page, int size);

    List<StockAlertResponse> getActiveAlerts(Long storeId);

    AlertStatsResponse getAlertStats(Long storeId);

    void markAsRead(Long alertId, Long storeId, Long userId);

    void markAllAsRead(Long storeId, Long userId);

    void resolveAlert(Long alertId, Long storeId, Long userId);

    void deleteAlert(Long alertId, Long storeId);

    StockAlertResponse createAlert(Long storeId,
                                   Long productId,
                                   Long batchId,
                                   com.zakisupermarket.entity.StockAlert.AlertType type,
                                   String title,
                                   String message,
                                   String severity);

    void generateLowStockAlerts(Long storeId);
    void generateExpiryAlerts(Long storeId);
}