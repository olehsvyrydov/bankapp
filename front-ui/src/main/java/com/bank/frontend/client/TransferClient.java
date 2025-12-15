package com.bank.frontend.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.transfer.TransferRequest;
import com.bank.common.dto.contracts.transfer.TransferResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "${clients.gateway.service-id:bank-app-gateway-service}", contextId = "transferClient")
public interface TransferClient {

    @PostMapping("/api/transfers")
    ApiResponse<TransferResponse> transfer(@RequestBody TransferRequest request);
}
