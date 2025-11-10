package com.bank.accounts.client;

import com.bank.common.dto.contracts.notifications.NotificationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationClientFallbackFactory implements FallbackFactory<NotificationClient> {

    @Override
    public NotificationClient create(Throwable cause) {
        return request -> log.error("Fallback: Failed to send notification to {}. Cause: {}",
            request.getUsername(), cause.getMessage());
    }
}

