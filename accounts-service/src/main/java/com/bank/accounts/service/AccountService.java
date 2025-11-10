package com.bank.accounts.service;

import com.bank.common.dto.contracts.accounts.*;

import java.util.List;

public interface AccountService {
    AccountDTO createAccount(CreateAccountRequest request);
    AccountDTO getAccountByUsername(String username);
    AccountDTO getAccountByEmail(String email);
    AccountDTO updateAccount(String username, UpdateAccountRequest request);
    void deleteAccount(String username);

    BankAccountDTO createBankAccount(String username, CreateBankAccountRequest request);
    List<BankAccountDTO> getBankAccountsByUsername(String username);
    BankAccountDTO getBankAccountById(Long id, String username);
    void deleteBankAccount(Long id, String username);

    BankAccountDTO updateBalance(UpdateBalanceRequest request);
    BankAccountDTO getBankAccountByIdPublic(Long id);
}
