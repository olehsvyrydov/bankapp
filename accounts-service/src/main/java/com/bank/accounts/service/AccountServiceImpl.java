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

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
@Slf4j
public class AccountServiceImpl implements AccountService {

    public static final String ACCOUNT_NOT_FOUND_MESSAGE = "Account not found";
    public static final String BANK_ACCOUNT_NOT_FOUND_MESSAGE = "Bank account not found";
    private final AccountRepository accountRepository;
    private final BankAccountRepository bankAccountRepository;
    private final NotificationClient notificationClient;
    private final AccountMapper accountMapper;

    public AccountServiceImpl(AccountRepository accountRepository,
        BankAccountRepository bankAccountRepository,
        NotificationClient notificationClient,
        AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.bankAccountRepository = bankAccountRepository;
        this.notificationClient = notificationClient;
        this.accountMapper = accountMapper;
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
            .balance(BigDecimal.ZERO)
            .build();

        bankAccountRepository.save(bankAccount);

        // Send notification
        notificationClient.sendNotification(NotificationRequest.builder()
            .username(request.getUsername())
            .message("Account created successfully")
            .type("INFO")
            .build());

        return accountMapper.toDTO(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountByUsername(String username) {
        Account account = accountRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ACCOUNT_NOT_FOUND_MESSAGE));
        return accountMapper.toDTO(account);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDTO getAccountByEmail(String email) {
        Account account = accountRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException("Account not found with email: " + email));
        return accountMapper.toDTO(account);
    }

    @Override
    public AccountDTO updateAccount(String username, UpdateAccountRequest request) {
        Account account = accountRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ACCOUNT_NOT_FOUND_MESSAGE));

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

        return accountMapper.toDTO(account);
    }

    @Override
    public void deleteAccount(String username) {
        Account account = accountRepository.findByUsername(username)
            .orElseThrow(() -> new BusinessException(ACCOUNT_NOT_FOUND_MESSAGE));

        // Check if any bank account has non-zero balance
        boolean hasBalance = account.getBankAccounts().stream()
            .anyMatch(ba -> ba.getBalance().compareTo(BigDecimal.ZERO) > 0);

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
            .orElseThrow(() -> new BusinessException(ACCOUNT_NOT_FOUND_MESSAGE));

        // Check if account already has a bank account with this currency
        boolean currencyExists = account.getBankAccounts().stream()
            .anyMatch(ba -> ba.getCurrency().equals(request.getCurrency()));

        if (currencyExists) {
            throw new BusinessException("Bank account with currency " + request.getCurrency() + " already exists");
        }

        BankAccount bankAccount = BankAccount.builder()
            .account(account)
            .currency(request.getCurrency())
            .balance(BigDecimal.ZERO)
            .build();

        bankAccount = bankAccountRepository.save(bankAccount);

        notificationClient.sendNotification(NotificationRequest.builder()
            .username(username)
            .message("Bank account created with currency " + request.getCurrency())
            .type("INFO")
            .build());

        return accountMapper.toBankAccountDTO(bankAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountDTO> getBankAccountsByUsername(String username) {
        List<BankAccount> bankAccounts = bankAccountRepository.findByAccountUsername(username);
        return accountMapper.toListBankAccountsDTO(bankAccounts);
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountDTO getBankAccountById(Long id, String username) {
        BankAccount bankAccount = bankAccountRepository.findByIdAndAccountUsername(id, username)
            .orElseThrow(() -> new BusinessException(BANK_ACCOUNT_NOT_FOUND_MESSAGE));
        return accountMapper.toBankAccountDTO(bankAccount);
    }

    @Override
    public void deleteBankAccount(Long id, String username) {
        BankAccount bankAccount = bankAccountRepository.findByIdAndAccountUsername(id, username)
            .orElseThrow(() -> new BusinessException(BANK_ACCOUNT_NOT_FOUND_MESSAGE));

        if (bankAccount.getBalance().compareTo(BigDecimal.ZERO) > 0) {
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
            .orElseThrow(() -> new BusinessException(BANK_ACCOUNT_NOT_FOUND_MESSAGE));

        BigDecimal newBalance = request.getOperation().apply(bankAccount.getBalance(), request.getAmount());
        bankAccount.setBalance(newBalance);

        bankAccount = bankAccountRepository.save(bankAccount);
        return accountMapper.toBankAccountDTO(bankAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountDTO getBankAccountByIdPublic(Long id) {
        BankAccount bankAccount = bankAccountRepository.findById(id)
            .orElseThrow(() -> new BusinessException(BANK_ACCOUNT_NOT_FOUND_MESSAGE));
        return accountMapper.toBankAccountDTO(bankAccount);
    }
}
