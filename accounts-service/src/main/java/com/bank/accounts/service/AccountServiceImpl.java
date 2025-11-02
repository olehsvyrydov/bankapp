package com.bank.accounts.service;

import com.bank.accounts.client.NotificationClient;
import com.bank.accounts.entity.Account;
import com.bank.accounts.entity.BankAccount;
import com.bank.accounts.mapper.AccountMapper;
import com.bank.accounts.repository.AccountRepository;
import com.bank.accounts.repository.BankAccountRepository;
import com.bank.common.dto.contracts.accounts.*;
import com.bank.common.dto.contracts.notifications.NotificationRequest;
import com.bank.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final BankAccountRepository bankAccountRepository;
    private final NotificationClient notificationClient;

    public AccountServiceImpl(AccountRepository accountRepository,
        BankAccountRepository bankAccountRepository,
        NotificationClient notificationClient) {
        this.accountRepository = accountRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.notificationClient = notificationClient;
    }

    @Override
    public AccountDTO createAccount(CreateAccountRequest request) {

        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("Username already exists");
        }

        Account account = Account.builder()
            .username(request.getUsername())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .email(request.getEmail())
            .birthDate(request.getBirthDate())
            .build();

        account = accountRepository.save(account);

        BankAccount bankAccount = BankAccount.builder()
            .account(account)
            .currency("RUB")
            .balance(0.0)
            .build();

        bankAccountRepository.save(bankAccount);

        // Send notification
        notificationClient.sendNotification(NotificationRequest.builder()
            .username(request.getUsername())
            .message("Account created successfully")
            .type("INFO")
            .build());

        return AccountMapper.toDTO(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountByUsername(String username) {
        Account account = accountRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("Account not found"));
        return AccountMapper.toDTO(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountByEmail(String email) {
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("Account not found with email: " + email));
        return AccountMapper.toDTO(account);
    }

    @Override
    public AccountDTO updateAccount(String username, UpdateAccountRequest request) {
        Account account = accountRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("Account not found"));

        account.setFirstName(request.getFirstName());
        account.setLastName(request.getLastName());
        account.setEmail(request.getEmail());
        account.setBirthDate(request.getBirthDate());

        account = accountRepository.save(account);

        notificationClient.sendNotification(NotificationRequest.builder()
            .username(username)
            .message("Account updated successfully")
            .type("INFO")
            .build());

        return AccountMapper.toDTO(account);
    }

    @Override
    public void deleteAccount(String username) {
        Account account = accountRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("Account not found"));

        // Check if any bank account has non-zero balance
        boolean hasBalance = account.getBankAccounts().stream()
            .anyMatch(ba -> ba.getBalance() > 0);

        if (hasBalance) {
            throw new BusinessException("Cannot delete account with non-zero balance");
        }

        accountRepository.delete(account);
        notificationClient.sendNotification(NotificationRequest.builder()
            .username(username)
            .message("Account deleted successfully")
            .type("INFO")
            .build());
    }

    @Override
    public BankAccountDTO createBankAccount(String username, CreateBankAccountRequest request) {
        Account account = accountRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException("Account not found"));

        // Check if account already has a bank account with this currency
        boolean currencyExists = account.getBankAccounts().stream()
            .anyMatch(ba -> ba.getCurrency().equals(request.getCurrency()));

        if (currencyExists) {
            throw new BusinessException("Bank account with currency " + request.getCurrency() + " already exists");
        }

        BankAccount bankAccount = BankAccount.builder()
            .account(account)
            .currency(request.getCurrency())
            .balance(0.0)
            .build();

        bankAccount = bankAccountRepository.save(bankAccount);

        notificationClient.sendNotification(NotificationRequest.builder()
            .username(username)
            .message("Bank account created with currency " + request.getCurrency())
            .type("INFO")
            .build());

        return AccountMapper.toBankAccountDTO(bankAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountDTO> getBankAccountsByUsername(String username) {
        List<BankAccount> bankAccounts = bankAccountRepository.findByAccountUsername(username);
        return bankAccounts.stream()
            .map(AccountMapper::toBankAccountDTO)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountDTO getBankAccountById(Long id, String username) {
        BankAccount bankAccount = bankAccountRepository.findByIdAndAccountUsername(id, username)
            .orElseThrow(() -> new BusinessException("Bank account not found"));
        return AccountMapper.toBankAccountDTO(bankAccount);
    }

    @Override
    public void deleteBankAccount(Long id, String username) {
        BankAccount bankAccount = bankAccountRepository.findByIdAndAccountUsername(id, username)
            .orElseThrow(() -> new BusinessException("Bank account not found"));

        if (bankAccount.getBalance() > 0) {
            throw new BusinessException("Cannot delete bank account with non-zero balance");
        }

        bankAccountRepository.delete(bankAccount);
        notificationClient.sendNotification(NotificationRequest.builder()
            .username(username)
            .message("Bank account deleted with currency " + bankAccount.getCurrency())
            .type("INFO")
            .build());
    }

    @Override
    public BankAccountDTO updateBalance(UpdateBalanceRequest request) {
        BankAccount bankAccount = bankAccountRepository.findById(request.getBankAccountId())
            .orElseThrow(() -> new BusinessException("Bank account not found"));

        if ("ADD".equals(request.getOperation())) {
            double newBalance = bankAccount.getBalance() + request.getAmount();
            bankAccount.setBalance(Math.round(newBalance * 100.0) / 100.0);
        } else if ("SUBTRACT".equals(request.getOperation())) {
            if (bankAccount.getBalance() < request.getAmount()) {
                throw new BusinessException("Insufficient balance");
            }
            double newBalance = bankAccount.getBalance() - request.getAmount();
            bankAccount.setBalance(Math.round(newBalance * 100.0) / 100.0);
        } else {
            throw new BusinessException("Invalid operation: " + request.getOperation());
        }

        bankAccount = bankAccountRepository.save(bankAccount);
        return AccountMapper.toBankAccountDTO(bankAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountDTO getBankAccountByIdPublic(Long id) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Bank account not found"));
        return AccountMapper.toBankAccountDTO(bankAccount);
    }
}
