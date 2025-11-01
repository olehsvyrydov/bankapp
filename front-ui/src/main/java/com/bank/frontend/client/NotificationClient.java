package com.bank.frontend.client;

import com.bank.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import javax.management.Notification;
import java.util.List;

@FeignClient(name = "notifications-service", url = "${clients.gateway-service.url}")
public interface NotificationClient {

    @GetMapping("/api/notifications/my")
    ApiResponse<List<Notification>> getNotifications();
}
