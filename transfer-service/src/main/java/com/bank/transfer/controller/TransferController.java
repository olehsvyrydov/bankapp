package com.bank.transfer.controller;

import com.bank.common.annotations.CurrentUsername;
import com.bank.common.dto.contracts.transfer.TransferRequest;
import com.bank.common.dto.contracts.transfer.TransferResponse;
import com.bank.transfer.service.TransferService;
import com.bank.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        @Valid @RequestBody TransferRequest request,
        @CurrentUsername String username) {
        TransferResponse response = transferService.processTransfer(request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Transfer completed"));
    }
}
