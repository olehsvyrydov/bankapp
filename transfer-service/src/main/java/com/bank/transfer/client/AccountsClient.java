package com.bank.transfer.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.BankAccountDTO;
import com.bank.common.dto.contracts.accounts.UpdateBalanceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient(
    name = "${clients.gateway.service-id:bank-app-gateway-service}",
    contextId = "accountsClient",
    fallbackFactory = AccountsClientFallbackFactory.class)
public interface AccountsClient {

    @GetMapping("/api/accounts/bank-accounts/{id}")
    ApiResponse<BankAccountDTO> getBankAccountById(@PathVariable("id") Long bankAccountId);

    @GetMapping("/api/accounts/by-email/{email}")
    ApiResponse<List<BankAccountDTO>> getBankAccountsByEmail(@PathVariable("email") String email);

    @PostMapping("/api/accounts/bank-accounts/update-balance")
    ApiResponse<BankAccountDTO> updateBalance(@RequestBody UpdateBalanceRequest request);
}
