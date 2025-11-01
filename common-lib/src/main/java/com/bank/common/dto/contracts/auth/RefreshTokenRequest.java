package com.bank.common.dto.contracts.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {
    @NotBlank(message = "{validation.refreshToken.required}")
    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("grant_type")
    private String grantType = "refresh_token";
}
