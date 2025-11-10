package com.bank.cash.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.BankAccountDTO;
import com.bank.common.dto.contracts.accounts.UpdateBalanceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(
    name = "gateway-service",
    contextId = "accountsClient"
)
public interface AccountsClient {

    @GetMapping("/api/accounts/bank-accounts/{id}")
    ApiResponse<BankAccountDTO> getBankAccount(@PathVariable("id") Long bankAccountId);

    @PostMapping("/api/accounts/bank-accounts/update-balance")
    ApiResponse<BankAccountDTO> updateBalance(@RequestBody UpdateBalanceRequest request);
}
