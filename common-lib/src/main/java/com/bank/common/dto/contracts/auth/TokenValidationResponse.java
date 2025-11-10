package com.bank.common.dto.contracts.auth;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TokenValidationResponse {
    private boolean valid;
    private String username;
    private Long userId;
    private List<String> roles;
}
