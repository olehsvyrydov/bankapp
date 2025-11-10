package com.bank.cash.controller;

import com.bank.common.dto.contracts.cash.CashOperationRequest;
import com.bank.common.dto.contracts.cash.CashOperationResponse;
import com.bank.cash.service.CashService;
import com.bank.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cash")
@Slf4j
public class CashController {

    private final CashService cashService;

    public CashController(CashService cashService) {
        this.cashService = cashService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<CashOperationResponse>> deposit(
        @Valid @RequestBody CashOperationRequest request) {
        log.debug("Requested deposit operation: {}", request);
        if (request.getType() != null && !"DEPOSIT".equalsIgnoreCase(request.getType())) {
            log.warn("Overriding unexpected cash operation type '{}' with DEPOSIT", request.getType());
        }
        request.setType("DEPOSIT");
        String username = resolveUsername();
        log.debug("Requested deposit operation username: {}", username);
        CashOperationResponse response = cashService.processOperation(request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Deposit completed"));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<ApiResponse<CashOperationResponse>> withdraw(
        @Valid @RequestBody CashOperationRequest request) {
        if (request.getType() != null && !request.getType().toUpperCase().startsWith("WITHDRAW")) {
            log.warn("Overriding unexpected cash operation type '{}' with WITHDRAWAL", request.getType());
        }
        request.setType("WITHDRAWAL");
        String username = resolveUsername();
        CashOperationResponse response = cashService.processOperation(request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Withdrawal completed"));
    }

    private String resolveUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            log.warn("resolveUsername: missing authentication {}", authentication);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        return authentication.getName();
    }
}
