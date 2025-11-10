
package com.bank.frontend.client;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.*;

public class AccountsClientFallback implements AccountsClient
{
    public static final String ERROR_MESSAGE = "Service unavailable, please try again later.";

    @Override
    public ApiResponse<AccountDTO> getMyAccount()
    {
        return ApiResponse.error(ERROR_MESSAGE);
    }

    @Override
    public ApiResponse<AccountDTO> createUserAccount(CreateAccountRequest request)
    {
        return ApiResponse.error(ERROR_MESSAGE);
    }

    @Override
    public ApiResponse<AccountDTO> updateUserAccount(UpdateAccountRequest request)
    {
        return ApiResponse.error(ERROR_MESSAGE);
    }

    @Override
    public ApiResponse<Void> changePassword(ChangePasswordRequest request)
    {
        return ApiResponse.error(ERROR_MESSAGE);
    }

    @Override
    public ApiResponse<BankAccountDTO> createBankAccount(CreateBankAccountRequest request)
    {
        return ApiResponse.error(ERROR_MESSAGE);
    }

    @Override
    public ApiResponse<Void> deleteBankAccount(Long bankAccountId)
    {
        return ApiResponse.error(ERROR_MESSAGE);
    }
}
