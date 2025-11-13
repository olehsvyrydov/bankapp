package com.bank.auth.client;

import com.bank.common.dto.contracts.notifications.NotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
    name = "${clients.gateway.service-id:bank-app-gateway-service}",
    fallback = NotificationClientFallback.class
)
public interface NotificationClient {

    @PostMapping("/api/notifications/send")
    void sendNotification(@RequestBody NotificationRequest request);
}
