package com.bank.auth.controller;

import com.bank.auth.client.NotificationClient;
import com.bank.common.dto.contracts.accounts.ChangePasswordRequest;
import com.bank.common.dto.contracts.auth.UserRegistrationRequest;
import com.bank.auth.service.UserService;
import com.bank.common.dto.contracts.notifications.NotificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController
{
    private final UserService userService;
    private final NotificationClient notificationClient;


    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserRegistrationRequest request)
    {
        try
        {
            var user = userService.registerUser(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                    "message", "User registered successfully",
                    "user", user
                ));
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/check/{username}")
    public ResponseEntity<?> checkUsername(@PathVariable String username)
    {
        boolean exists = userService.usernameExists(username);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest request,
        Authentication authentication)
    {
        try
        {
            String username = authentication.getName();
            // Verify the username from token matches the request
            if (!username.equals(request.getUsername()))
            {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Cannot change password for another user"));
            }
            userService.changePassword(username, request.getNewPassword());
            notificationClient.sendNotification(NotificationRequest.builder()
                .username(username)
                .message("&#x2714; Your password has been changed successfully.")
                .type("INFO")
                .build());
            return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
        }
        catch (Exception e)
        {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
}
