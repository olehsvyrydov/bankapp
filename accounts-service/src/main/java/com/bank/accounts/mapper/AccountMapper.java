package com.bank.accounts.mapper;

import com.bank.common.dto.contracts.accounts.AccountDTO;
import com.bank.common.dto.contracts.accounts.BankAccountDTO;
import com.bank.accounts.entity.Account;
import com.bank.accounts.entity.BankAccount;

public class AccountMapper {

    private AccountMapper(){}

    public static AccountDTO toDTO(Account account) {
        return AccountDTO.builder()
            .id(account.getId())
            .username(account.getUsername())
            .firstName(account.getFirstName())
            .lastName(account.getLastName())
            .email(account.getEmail())
            .birthDate(account.getBirthDate())
            .bankAccounts(account.getBankAccounts().stream()
                .map(AccountMapper::toBankAccountDTO)
                .toList())
            .build();
    }

    public static BankAccountDTO toBankAccountDTO(BankAccount bankAccount) {
        return BankAccountDTO.builder()
            .id(bankAccount.getId())
            .currency(bankAccount.getCurrency())
            .balance(bankAccount.getBalance())
            .accountUsername(bankAccount.getAccount().getUsername())
            .build();
    }
}
