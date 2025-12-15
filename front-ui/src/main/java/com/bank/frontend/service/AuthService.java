package com.bank.frontend.service;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.ChangePasswordRequest;
import com.bank.common.auth.*;
import com.bank.common.exception.BusinessException;
import com.bank.frontend.client.AuthServiceClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class AuthService
{

    private final AuthServiceClient authClient;

    public ApiResponse<Void> register(RegisterRequest request)
    {
        try
        {
            return ApiResponse.success(authClient.registerUser(request));
        }
        catch (Exception e)
        {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TokenResponse> login(LoginRequest request)
    {
        try
        {
            return ApiResponse.success(authClient.loginUser(request));
        }
        catch (Exception e)
        {
            return ApiResponse.error(e.getMessage());
        }
    }

    public ApiResponse<TokenResponse> refreshToken(String refreshToken)
    {
        try
        {
            return ApiResponse.success(authClient.refreshToken(new RefreshTokenRequest(refreshToken, "refresh_token")));
        }
        catch (Exception e)
        {
            return ApiResponse.error(e.getMessage());
        }
    }

    public void changePassword(@Valid ChangePasswordRequest request, String token){
        try {
            // Use authUserClient which sends JWT token instead of BasicAuth
            authClient.changePassword("Bearer " + token, request);
            log.info("✔ Password changed successfully for user {}", request.getUsername());
        }
        catch (Exception e) {
            log.error("✖ Password change failed for user {}: {}", request.getUsername(), e.getMessage());
            throw new BusinessException("Failed to change password");
        }
    }
}
