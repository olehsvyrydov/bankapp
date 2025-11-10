package com.bank.transfer.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.BankAccountDTO;
import com.bank.common.dto.contracts.accounts.UpdateBalanceRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class AccountsClientFallbackFactory implements FallbackFactory<AccountsClient> {

    @Override
    public AccountsClient create(Throwable cause) {
        return new AccountsClient() {
            @Override
            public ApiResponse<BankAccountDTO> getBankAccountById(Long bankAccountId) {
                log.error("Fallback: Failed to get bank account by id {}. Cause: {}",
                    bankAccountId, cause.getMessage());
                return ApiResponse.error("Service unavailable");
            }

            @Override
            public ApiResponse<List<BankAccountDTO>> getBankAccountsByEmail(String email) {
                log.error("Fallback: Failed to get bank accounts by email {}. Cause: {}",
                    email, cause.getMessage());
                return ApiResponse.error("Service unavailable");
            }

            @Override
            public ApiResponse<BankAccountDTO> updateBalance(UpdateBalanceRequest request) {
                log.error("Fallback: Failed to update balance for account {}. Cause: {}",
                    request.getBankAccountId(), cause.getMessage());
                return ApiResponse.error("Service unavailable");
            }
        };
    }
}

