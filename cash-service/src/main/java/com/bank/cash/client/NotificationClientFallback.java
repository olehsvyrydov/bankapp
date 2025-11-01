package com.bank.cash.client;

import com.bank.common.dto.contracts.notifications.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationClientFallback implements NotificationClient {

    @Override
    public void sendNotification(NotificationRequest request) {
        log.warn("Notification service unavailable; user={}, message={}",
            request.getUsername(), request.getMessage());
    }
}
