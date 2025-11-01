package com.bank.generator.client;

import com.bank.common.dto.contracts.exchange.ExchangeRateDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "exchange-service", url = "${clients.gateway-service.url}")
public interface ExchangeClient {

    @PostMapping("/api/exchange/rates")
    void updateRate(@RequestBody ExchangeRateDTO request);
}
