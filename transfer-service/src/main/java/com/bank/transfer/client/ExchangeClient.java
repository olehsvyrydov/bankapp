package com.bank.transfer.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.exchange.ConversionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
    name = "exchange-service", url = "${clients.gateway-service.url}",
    fallback = ExchangeClientFallback.class
)
public interface ExchangeClient {

    @PostMapping("/api/exchange/convert")
    ApiResponse<Double> convert(@RequestBody ConversionRequest request);
}
