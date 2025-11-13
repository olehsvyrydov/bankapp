package com.bank.frontend.client;

import com.bank.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import javax.management.Notification;
import java.util.List;

@FeignClient(name = "${clients.gateway.service-id:bank-app-gateway-service}", contextId = "notificationClient")
public interface NotificationClient {

    @GetMapping("/api/notifications/my")
    ApiResponse<List<Notification>> getNotifications();
}
