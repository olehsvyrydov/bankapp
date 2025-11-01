package com.bank.frontend.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.cash.CashOperationRequest;
import com.bank.common.dto.contracts.cash.CashOperationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "cash-service", url = "${clients.gateway-service.url}")
public interface CashClient
{

    @PostMapping("/api/cash/deposit")
    ApiResponse<CashOperationResponse> deposit(@RequestBody CashOperationRequest request);

    @PostMapping("/api/cash/withdraw")
    ApiResponse<CashOperationResponse> withdraw(@RequestBody CashOperationRequest request);
}
