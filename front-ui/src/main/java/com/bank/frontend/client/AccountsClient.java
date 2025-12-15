package com.bank.frontend.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "${clients.gateway.service-id:bank-app-gateway-service}", contextId = "accountsClient", fallback = AccountsClientFallback.class)
public interface AccountsClient {

    @GetMapping("/api/accounts/me")
    ApiResponse<AccountDTO> getMyAccount();

    @PostMapping("/api/accounts/register")
    ApiResponse<AccountDTO> createUserAccount(@RequestBody CreateAccountRequest request);

    @PutMapping("/api/accounts/me")
    ApiResponse<AccountDTO> updateUserAccount(@RequestBody UpdateAccountRequest request);

    @PostMapping("/api/accounts/me/password")
    ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request);

    @PostMapping("/api/accounts/me/bank-accounts")
    ApiResponse<BankAccountDTO> createBankAccount(@RequestBody CreateBankAccountRequest request);

    @DeleteMapping("/api/accounts/me/bank-accounts/{id}")
    ApiResponse<Void> deleteBankAccount(@PathVariable("id") Long bankAccountId);
}
