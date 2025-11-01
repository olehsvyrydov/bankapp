package com.bank.transfer.controller;

import com.bank.common.dto.contracts.transfer.TransferRequest;
import com.bank.common.dto.contracts.transfer.TransferResponse;
import com.bank.transfer.service.TransferService;
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
@RequestMapping("/api/transfers")
@Slf4j
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
        @Valid @RequestBody TransferRequest request) {
        String username = resolveUsername();
        TransferResponse response = transferService.processTransfer(request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Transfer completed"));
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
