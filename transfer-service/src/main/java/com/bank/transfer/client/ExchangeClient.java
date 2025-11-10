package com.bank.transfer.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.exchange.ConversionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(
    name = "gateway-service",
    contextId = "exchangeClient",
    fallbackFactory = ExchangeClientFallbackFactory.class
)
public interface ExchangeClient {

    @PostMapping("/api/exchange/convert")
    ApiResponse<BigDecimal> convert(@RequestBody ConversionRequest request);
}
