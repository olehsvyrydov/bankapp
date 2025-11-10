package com.bank.common.dto.contracts.auth;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AuthResponse {
    // Getters and Setters
    private String token;
    private String type;
    private String username;
    private String message;

    public AuthResponse() {}

    public AuthResponse(String message) {
        this.message = message;
    }

    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
    }

}
