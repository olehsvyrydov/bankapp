package com.bank.accounts.service;

import com.bank.accounts.client.NotificationClient;
import com.bank.accounts.entity.Account;
import com.bank.accounts.repository.AccountRepository;
import com.bank.accounts.repository.BankAccountRepository;
import com.bank.common.dto.contracts.accounts.AccountDTO;
import com.bank.common.dto.contracts.accounts.CreateAccountRequest;
import com.bank.common.dto.contracts.notifications.NotificationRequest;
import com.bank.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    void testCreateAccount_UnderAge() {
        validRequest.setBirthDate(LocalDate.now().minusYears(17));

        assertThrows(Exception.class, () -> accountService.createAccount(validRequest));
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
        mockAccount.getBankAccounts().add(com.bank.accounts.entity.BankAccount.builder()
            .balance(100.0)
            .build());
        when(accountRepository.findByUsername(anyString())).thenReturn(Optional.of(mockAccount));

        assertThrows(BusinessException.class, () -> accountService.deleteAccount("testuser"));
        verify(accountRepository, never()).delete(any(Account.class));
    }
}
