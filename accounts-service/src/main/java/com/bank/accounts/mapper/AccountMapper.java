package com.bank.accounts.mapper;

import com.bank.common.dto.contracts.accounts.AccountDTO;
import com.bank.common.dto.contracts.accounts.BankAccountDTO;
import com.bank.accounts.entity.Account;
import com.bank.accounts.entity.BankAccount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.mapstruct.MappingConstants.ComponentModel.*;

@Mapper(componentModel = SPRING)
public interface AccountMapper {
    AccountMapper INSTANCE = Mappers.getMapper(AccountMapper.class);

    @Mapping(target = "bankAccounts", source = "bankAccounts", qualifiedByName = "mpsToListBankAccountsDTO")
    AccountDTO toDTO(Account account);

    @Mapping(target = "accountUsername", source = "account.username")
    BankAccountDTO toBankAccountDTO(BankAccount bankAccount);

    @Named("mpsToListBankAccountsDTO")
    default List<BankAccountDTO> toListBankAccountsDTO(List<BankAccount> bankAccounts) {
        return bankAccounts.stream()
            .map(this::toBankAccountDTO)
            .toList();
    }
}
