
package com.bank.frontend.client;

import com.bank.common.dto.contracts.accounts.ChangePasswordRequest;
import com.bank.common.auth.*;
import com.bank.frontend.config.AuthClientConfig;
import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "${clients.auth.service-id:bank-app-auth-server}", contextId = "authServiceClient", configuration = AuthClientConfig.class)
public interface AuthServiceClient {

    @PostMapping("/api/auth/register")
    Void registerUser(@RequestBody RegisterRequest registration);

    @PostMapping("/api/auth/login")
    TokenResponse loginUser(@RequestBody LoginRequest login);

    @PostMapping("/oauth2/token")
    TokenResponse authenticate(@RequestBody MultiValueMap<String, String> formData);

    @GetMapping("/api/users/me")
    UserDTO getCurrentUser(@RequestHeader("Authorization") String token);

    @PostMapping("/api/auth/refresh")
    TokenResponse refreshToken(@RequestBody RefreshTokenRequest request);

    @PostMapping("/api/users/change-password")
    void changePassword(@RequestHeader("Authorization") String token,
                        @Valid @RequestBody ChangePasswordRequest request);
}
