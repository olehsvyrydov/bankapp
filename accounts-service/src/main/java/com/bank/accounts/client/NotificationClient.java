package com.bank.accounts.client;

import com.bank.common.dto.contracts.notifications.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "${clients.gateway.service-id:bank-app-gateway-service}",
    contextId = "notificationClient",
    fallback = NotificationClientFallback.class
)
public interface NotificationClient {

    @PostMapping("/api/notifications/send")
    void sendNotification(@RequestBody NotificationRequest request);
}
