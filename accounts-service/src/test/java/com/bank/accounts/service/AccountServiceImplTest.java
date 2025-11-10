package com.bank.accounts.service;

import com.bank.accounts.client.NotificationClient;
import com.bank.accounts.entity.Account;
import com.bank.accounts.entity.BankAccount;
import com.bank.accounts.mapper.AccountMapper;
import com.bank.accounts.repository.AccountRepository;
import com.bank.accounts.repository.BankAccountRepository;
import com.bank.common.dto.contracts.accounts.AccountDTO;
import com.bank.common.dto.contracts.accounts.BankAccountDTO;
import com.bank.common.dto.contracts.accounts.BankOperation;
import com.bank.common.dto.contracts.accounts.CreateAccountRequest;
import com.bank.common.dto.contracts.accounts.UpdateBalanceRequest;
import com.bank.common.dto.contracts.notifications.NotificationRequest;
import com.bank.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @Mock
    private NotificationClient notificationClient;

    @Spy
    private final AccountMapper accountMapper = AccountMapper.INSTANCE;

    @InjectMocks
    private AccountServiceImpl accountService;

    private CreateAccountRequest validRequest;
    private Account mockAccount;

    @BeforeEach
    void setUp() {
        validRequest = CreateAccountRequest.builder()
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .email("test@example.com")
            .birthDate(LocalDate.of(2000, 1, 1))
            .build();

        mockAccount = Account.builder()
            .id(1L)
            .username("testuser")
            .firstName("Test")
            .lastName("User")
            .email("test@example.com")
            .birthDate(LocalDate.of(2000, 1, 1))
            .build();
    }

    @Test
    void testCreateAccount_Success() {
        when(accountRepository.existsByUsername(anyString())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(mockAccount);
        doNothing().when(notificationClient).sendNotification(any(NotificationRequest.class));

        AccountDTO result = accountService.createAccount(validRequest);

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
        verify(accountRepository).save(any(Account.class));
        verify(notificationClient).sendNotification(any(NotificationRequest.class));
    }

    @Test
    void testCreateAccount_UsernameExists() {
        when(accountRepository.existsByUsername(anyString())).thenReturn(true);

        assertThrows(BusinessException.class, () -> accountService.createAccount(validRequest));
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    void testGetAccountByUsername_Success() {
        when(accountRepository.findByUsername(anyString())).thenReturn(Optional.of(mockAccount));

        AccountDTO result = accountService.getAccountByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testGetAccountByUsername_NotFound() {
        when(accountRepository.findByUsername(anyString())).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> accountService.getAccountByUsername("testuser"));
    }

    @Test
    void testDeleteAccount_WithBalance() {
        mockAccount.getBankAccounts().add(BankAccount.builder()
            .balance(BigDecimal.valueOf(100.0))
            .build());
        when(accountRepository.findByUsername(anyString())).thenReturn(Optional.of(mockAccount));

        assertThrows(BusinessException.class, () -> accountService.deleteAccount("testuser"));
        verify(accountRepository, never()).delete(any(Account.class));
    }

    @Test
    void testUpdateBalance_Deposit_Success() {
        BankAccount bankAccount = BankAccount.builder()
            .id(1L)
            .balance(BigDecimal.valueOf(100.0))
            .currency("USD")
            .account(mockAccount)
            .build();

        UpdateBalanceRequest request = UpdateBalanceRequest.builder()
            .bankAccountId(1L)
            .amount(BigDecimal.valueOf(50.0))
            .operation(BankOperation.ADD)
            .build();

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(bankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount saved = invocation.getArgument(0);
            assertEquals(BigDecimal.valueOf(150.0), saved.getBalance());
            return saved;
        });

        BankAccountDTO result = accountService.updateBalance(request);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(150.0), result.getBalance());
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    void testUpdateBalance_Withdrawal_Success() {
        BankAccount bankAccount = BankAccount.builder()
            .id(1L)
            .balance(BigDecimal.valueOf(100.0))
            .currency("USD")
            .account(mockAccount)
            .build();

        UpdateBalanceRequest request = UpdateBalanceRequest.builder()
            .bankAccountId(1L)
            .amount(BigDecimal.valueOf(30.0))
            .operation(BankOperation.SUBTRACT)
            .build();

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(bankAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount saved = invocation.getArgument(0);
            assertEquals(BigDecimal.valueOf(70.0), saved.getBalance());
            return saved;
        });

        BankAccountDTO result = accountService.updateBalance(request);

        assertNotNull(result);
        assertEquals(BigDecimal.valueOf(70.0), result.getBalance());
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    void testUpdateBalance_Withdrawal_InsufficientBalance() {
        BankAccount bankAccount = BankAccount.builder()
            .id(1L)
            .balance(BigDecimal.valueOf(50.0))
            .currency("USD")
            .account(mockAccount)
            .build();

        UpdateBalanceRequest request = UpdateBalanceRequest.builder()
            .bankAccountId(1L)
            .amount(BigDecimal.valueOf(100.0))
            .operation(BankOperation.SUBTRACT)
            .build();

        when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(bankAccount));

        assertThrows(BusinessException.class, () -> accountService.updateBalance(request));
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    void testUpdateBalance_BankAccountNotFound() {
        UpdateBalanceRequest request = UpdateBalanceRequest.builder()
            .bankAccountId(999L)
            .amount(BigDecimal.valueOf(50.0))
            .operation(BankOperation.ADD)
            .build();

        when(bankAccountRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> accountService.updateBalance(request));
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }
}
