package com.bank.cash.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
    name = "blocker-service",
    url = "${clients.gateway-service.url}",
    fallback = BlockerClientFallback.class
)
public interface BlockerClient {

    @PostMapping("/api/blocker/check")
    Map<String, Object> checkOperation(@RequestBody Map<String, Object> request);
}
