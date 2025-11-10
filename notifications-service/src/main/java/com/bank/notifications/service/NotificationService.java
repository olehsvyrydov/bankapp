package com.bank.notifications.service;

import com.bank.common.dto.contracts.notifications.NotificationRequest;
import com.bank.notifications.entity.Notification;

import java.util.List;

public interface NotificationService {
    void sendNotification(NotificationRequest request);
    List<Notification> getUserNotifications(String username);
    void markAsRead(Long notificationId);
}
