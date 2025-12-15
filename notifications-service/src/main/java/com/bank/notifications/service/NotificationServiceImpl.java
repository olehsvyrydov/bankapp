package com.bank.notifications.service;

import com.bank.common.dto.contracts.notifications.NotificationRequest;
import com.bank.common.metrics.CustomMetricsService;
import com.bank.notifications.entity.Notification;
import com.bank.notifications.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CustomMetricsService metricsService;

    @Value("${notification.type:console}")
    private String notificationType;

    public NotificationServiceImpl(NotificationRepository notificationRepository, CustomMetricsService metricsService) {
        this.notificationRepository = notificationRepository;
        this.metricsService = metricsService;
    }

    @Override
    public void sendNotification(NotificationRequest request) {
        try {
            // Save notification to database
            Notification notification = Notification.builder()
                .username(request.getUsername())
                .message(request.getMessage())
                .type(request.getType() != null ? request.getType() : "INFO")
                .read(false)
                .build();

            notificationRepository.save(notification);

            // Console notification (could be email, SMS, etc.)
            System.out.println("=== NOTIFICATION ===");
            System.out.println("To: " + request.getUsername());
            System.out.println("Message: " + request.getMessage());
            System.out.println("Type: " + notification.getType());
            System.out.println("====================");
        } catch (Exception ex) {
            log.error("Failed to send notification to user: {}", request.getUsername(), ex);
            metricsService.recordFailedNotification(request.getUsername());
            throw ex;
        }
    }

    @Override
    public List<Notification> getUserNotifications(String username) {
        return notificationRepository.findByUsernameOrderByCreatedAtDesc(username);
    }

    @Override
    public void markAsRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
}
