package com.bank.transfer.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.BankAccountDTO;
import com.bank.common.dto.contracts.accounts.UpdateBalanceRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AccountsClientFallback implements AccountsClient {

    @Override
    public ApiResponse<BankAccountDTO> getBankAccountById(Long bankAccountId) {
        log.error("Fallback: Failed to get bank account by id {}", bankAccountId);
        return ApiResponse.error("Service unavailable");
    }

    @Override
    public ApiResponse<List<BankAccountDTO>> getBankAccountsByEmail(String email) {
        log.error("Fallback: Failed to get bank accounts by email {}", email);
        return ApiResponse.error("Service unavailable");
    }

    @Override
    public ApiResponse<BankAccountDTO> updateBalance(UpdateBalanceRequest request) {
        log.error("Fallback: Failed to update balance for account {}", request.getBankAccountId());
        return ApiResponse.error("Service unavailable");
    }
}
