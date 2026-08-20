package com.zakisupermarket.service;

import com.zakisupermarket.dto.request.NotificationRequest;
import com.zakisupermarket.dto.response.NotificationResponse;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.util.List;

public interface NotificationService {

    NotificationResponse createNotification(NotificationRequest request);
    NotificationResponse markAsRead(Long notificationId, Long userId, Long storeId);

    int markAllAsRead(Long storeId, Long userId);

    Page<NotificationResponse> getUserNotifications(Long storeId, Long userId, int page, int size);
    List<NotificationResponse> getUnreadNotifications(Long storeId, Long userId);
    Long getUnreadCount(Long storeId, Long userId);

    void deleteNotification(Long notificationId, Long userId, Long storeId);

    void checkAndCreateLowStockAlerts(Long storeId);
    void checkAndCreateExpiryAlerts(Long storeId);
    void checkAndCreateBackupReminders(Long storeId);

    void notifySaleCompleted(Long storeId, Long saleId, BigDecimal totalAmount);
    void notifyExpenseAdded(Long storeId, Long expenseId, BigDecimal amount);
    void notifySecurityAlert(Long lockedOutUserId);
}