package com.bank.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private String error;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private Map<String, String> validationErrors;

    public static ErrorResponse of(String error, String message, int status) {
        return ErrorResponse.builder()
            .error(error)
            .message(message)
            .status(status)
            .timestamp(LocalDateTime.now())
            .build();
    }
}
