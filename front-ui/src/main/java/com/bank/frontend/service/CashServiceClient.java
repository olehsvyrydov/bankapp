package com.bank.frontend.service;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.cash.CashOperationRequest;
import com.bank.common.dto.contracts.cash.CashOperationResponse;
import com.bank.common.exception.RemoteOperationException;
import com.bank.frontend.client.CashClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class CashServiceClient {

    private final CashClient cashClient;

    public CashServiceClient(CashClient cashClient) {
        this.cashClient = cashClient;
    }

    public void deposit(CashOperationRequest request) {
        ApiResponse<CashOperationResponse> response = cashClient.deposit(request);
        if (!response.isSuccess()) {
            log.error("Deposit failed: {}", response.getMessage());
            throw new RemoteOperationException("Deposit failed: " + response.getMessage());
        }
    }

    public void withdraw(CashOperationRequest request) {
        ApiResponse<CashOperationResponse> response = cashClient.withdraw(request);
        if (!response.isSuccess()) {
            log.error("Withdrawal failed: {}", response.getMessage());
            throw new RemoteOperationException("Withdrawal failed: " + response.getMessage());
        }
    }
}
