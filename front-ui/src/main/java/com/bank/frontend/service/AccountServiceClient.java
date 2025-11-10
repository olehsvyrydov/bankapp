package com.bank.frontend.service;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.*;
import com.bank.common.exception.ResourceNotFoundException;
import com.bank.frontend.client.AccountsClient;
import com.bank.frontend.exceptions.AccountsManipulationException;
import com.bank.frontend.exceptions.UnauthorizedException;
import feign.FeignException;
import org.springframework.stereotype.Service;


@Service
public class AccountServiceClient {

    private final AccountsClient accountsClient;

    public AccountServiceClient(AccountsClient accountsClient) {
        this.accountsClient = accountsClient;
    }

    public AccountDTO getAccountDetails() {
        try {
            ApiResponse<AccountDTO> response = accountsClient.getMyAccount();
            if (!response.isSuccess()) {
                throw new ResourceNotFoundException("Failed to fetch account details: " + response.getMessage());
            }
            return response.getData();
        } catch (FeignException.Unauthorized ex) {
            throw new UnauthorizedException("Session expired or token invalid");
        } catch (FeignException ex) {
            throw new ResourceNotFoundException("Failed to fetch account details: " + ex.getMessage());
        }
    }

    public void createAccount(CreateAccountRequest request) {
        try {
            ApiResponse<AccountDTO> response = accountsClient.createUserAccount(request);
            if (!response.isSuccess()) {
                throw new AccountsManipulationException("Failed to create account: " + response.getMessage());
            }
        } catch (FeignException.Unauthorized ex) {
            throw new UnauthorizedException("Session expired or token invalid");
        } catch (FeignException ex) {
            throw new AccountsManipulationException("Failed to create account: " + ex.getMessage());
        }
    }


    public void updateAccount(UpdateAccountRequest request) {
        try {
            ApiResponse<AccountDTO> response = accountsClient.updateUserAccount(request);
            if (!response.isSuccess()) {
                throw new AccountsManipulationException("Failed to update account: " + response.getMessage());
            }
        } catch (FeignException.Unauthorized ex) {
            throw new UnauthorizedException("Session expired or token invalid");
        } catch (FeignException ex) {
            throw new AccountsManipulationException("Failed to update account: " + ex.getMessage());
        }
    }

    public void createBankAccount(CreateBankAccountRequest request) {
        try {
            ApiResponse<BankAccountDTO> response = accountsClient.createBankAccount(request);
            if (!response.isSuccess()) {
                throw new AccountsManipulationException("Failed to create bank account: " + response.getMessage());
            }
        } catch (FeignException.Unauthorized ex) {
            throw new UnauthorizedException("Session expired or token invalid");
        } catch (FeignException ex) {
            throw new AccountsManipulationException("Failed to create bank account: " + ex.getMessage());
        }
    }

    public void deleteBankAccount(Long bankAccountId) {
        try {
            ApiResponse<Void> response = accountsClient.deleteBankAccount(bankAccountId);
            if (!response.isSuccess()) {
                throw new AccountsManipulationException("Failed to delete bank account: " + response.getMessage());
            }
        } catch (FeignException.Unauthorized ex) {
            throw new UnauthorizedException("Session expired or token invalid");
        } catch (FeignException ex) {
            throw new AccountsManipulationException("Failed to delete bank account: " + ex.getMessage());
        }
    }
}
