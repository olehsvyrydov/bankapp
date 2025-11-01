package com.bank.frontend.service;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.transfer.TransferRequest;
import com.bank.common.dto.contracts.transfer.TransferResponse;
import com.bank.frontend.client.TransferClient;
import org.springframework.stereotype.Service;

@Service
public class TransferServiceClient {

    private final TransferClient transferClient;

    public TransferServiceClient(TransferClient transferClient) {
        this.transferClient = transferClient;
    }

    public void transfer(TransferRequest request) {
        ApiResponse<TransferResponse> response = transferClient.transfer(request);
        if (!response.isSuccess()) {
            throw new RuntimeException("Transfer failed: " + response.getMessage());
        }
    }
}
