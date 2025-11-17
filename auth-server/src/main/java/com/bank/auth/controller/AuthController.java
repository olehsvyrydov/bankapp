package com.bank.auth.controller;

import com.bank.auth.service.TokenService;
import com.bank.auth.service.UserService;
import com.bank.common.auth.AuthResponse;
import com.bank.common.auth.LoginRequest;
import com.bank.common.auth.RefreshTokenRequest;
import com.bank.common.auth.TokenResponse;
import com.bank.common.auth.TokenValidationResponse;
import com.bank.common.auth.UserRegistrationRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TokenService tokenService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegistrationRequest request) {
        try {
            var registeredUser = userService.registerUser(request);
            TokenResponse tokens = tokenService.createTokenResponse(registeredUser);
            return ResponseEntity.status(HttpStatus.CREATED).body(tokens);
        } catch (IllegalArgumentException ex) {
            log.warn("User registration failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AuthResponse(ex.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

            TokenResponse response = tokenService.createTokenResponse(
                userService.findByUsername(authentication.getName()),
                request.isRememberMe());
            return ResponseEntity.ok(response);
        } catch (AuthenticationException ex) {
            log.warn("Authentication failed for user {}: {}", request.getUsername(), ex.getMessage());
            throw new BadCredentialsException("Invalid username or password");
        }
    }

    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return ResponseEntity.ok(tokenService.validateToken(token));
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(TokenValidationResponse.builder().valid(false).build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        try {
            return ResponseEntity.ok(tokenService.refreshAccessToken(request.getRefreshToken()));
        } catch (Exception ex) {
            log.warn("Token refresh failed: {}", ex.getMessage());
            throw new BadCredentialsException("Invalid refresh token");
        }
    }
}
