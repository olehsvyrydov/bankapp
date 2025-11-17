package com.bank.notifications.controller;

import com.bank.common.annotations.CurrentUsername;
import com.bank.notifications.entity.Notification;
import com.bank.notifications.service.NotificationService;
import com.bank.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(@CurrentUsername String username) {
        List<Notification> notifications = notificationService.getUserNotifications(username);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Notification marked as read"));
    }
}
