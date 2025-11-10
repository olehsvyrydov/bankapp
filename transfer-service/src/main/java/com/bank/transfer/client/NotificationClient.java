package com.bank.transfer.client;

import com.bank.common.dto.contracts.notifications.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "gateway-service",
    contextId = "notificationClient",
    fallbackFactory = NotificationClientFallbackFactory.class
)
public interface NotificationClient {

    @PostMapping("/api/notifications/send")
    void sendNotification(@RequestBody NotificationRequest request);
}
