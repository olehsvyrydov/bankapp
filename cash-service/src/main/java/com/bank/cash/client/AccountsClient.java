package com.bank.cash.client;

import com.bank.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
    name = "accounts-service", url = "${clients.gateway-service.url}"
)
public interface AccountsClient {

    @GetMapping("/api/accounts/bank-accounts/{id}")
    ApiResponse<Map<String, Object>> getBankAccount(@PathVariable("id") Long bankAccountId);

    @PostMapping("/api/accounts/bank-accounts/update-balance")
    ApiResponse<Map<String, Object>> updateBalance(@RequestBody Map<String, Object> request);
}
