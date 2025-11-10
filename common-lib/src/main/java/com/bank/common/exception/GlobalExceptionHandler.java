package com.bank.common.exception;

import com.bank.common.dto.ErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex) {
        ErrorResponse error = ErrorResponse.of(
            "Business Error",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponse error = ErrorResponse.of(
            "Resource Not Found",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        ErrorResponse error = ErrorResponse.builder()
            .error("Validation Error")
            .message(ex.getMessage())
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .validationErrors(ex.getErrors())
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse error = ErrorResponse.builder()
            .error("Validation Error")
            .message("Invalid input data")
            .status(HttpStatus.BAD_REQUEST.value())
            .timestamp(LocalDateTime.now())
            .validationErrors(errors)
            .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidEnum(HttpMessageNotReadableException ex) {
        String message = "Invalid operation value";

        if (ex.getCause() instanceof InvalidFormatException cause && cause.getTargetType() != null && cause.getTargetType().isEnum()) {
                Object[] enumValues = cause.getTargetType().getEnumConstants();
                message = String.format("Invalid operation value. Allowed values: %s",
                    Arrays.toString(enumValues));
            }

        return ResponseEntity
            .badRequest()
            .body(ErrorResponse.builder()
                .error("Validation Error")
                .message(message)
                .status(400)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ErrorResponse error = ErrorResponse.of(
            "Invalid Argument",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RemoteOperationException.class)
    public ResponseEntity<ErrorResponse> handleRemoteOperationException(RemoteOperationException ex) {
        ErrorResponse error = ErrorResponse.of(
            "Remote Operation Error",
            ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex) {
        ErrorResponse error = ErrorResponse.of(
            "Not Found",
            ex.getMessage(),
            HttpStatus.NOT_FOUND.value()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        ErrorResponse error = ErrorResponse.of(
            ex.getStatusCode().is4xxClientError() ? "Client Error" : "Server Error",
            ex.getReason() != null ? ex.getReason() : "Request failed",
            ex.getStatusCode().value()
        );
        return ResponseEntity.status(ex.getStatusCode()).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse error = ErrorResponse.of(
            "Internal Server Error",
            ex.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
