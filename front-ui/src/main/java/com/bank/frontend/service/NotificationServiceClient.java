package com.bank.frontend.service;

import com.bank.common.dto.ApiResponse;
import com.bank.frontend.client.NotificationClient;
import org.springframework.stereotype.Service;

import javax.management.Notification;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceClient {

    private final NotificationClient notificationClient;

    public NotificationServiceClient(NotificationClient notificationClient) {
        this.notificationClient = notificationClient;
    }

    public List<Notification> getNotifications() {
        ApiResponse<List<Notification>> response = notificationClient.getNotifications();
        if (!response.isSuccess()) {
            throw new RuntimeException("Failed to fetch notifications: " + response.getMessage());
        }
        return response.getData();
    }
}
