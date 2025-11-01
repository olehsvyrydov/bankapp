package com.bank.transfer.client;

import com.bank.common.dto.contracts.blocker.BlockCheckRequest;
import com.bank.common.dto.contracts.blocker.BlockCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(
    name = "blocker-service",
    url = "${clients.gateway-service.url}",
    fallback = BlockerClientFallback.class
)
public interface BlockerClient {

    @PostMapping("/api/blocker/check")
    BlockCheckResponse checkOperation(@RequestBody BlockCheckRequest request);
}
