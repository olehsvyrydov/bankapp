package com.bank.frontend.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "${clients.gateway.service-id:bank-app-gateway-service}", contextId = "exchangeClient")
public interface ExchangeClient {

    @GetMapping("/api/exchange/rates")
    ApiResponse<List<ExchangeRateDTO>> getExchangeRates();
}
