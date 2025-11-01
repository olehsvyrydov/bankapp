package com.bank.frontend.controller;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.CreateBankAccountRequest;
import com.bank.frontend.service.AccountServiceClient;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BankAccountController Tests")
class BankAccountControllerTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private HttpSession session;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private BankAccountController bankAccountController;

    private CreateBankAccountRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = CreateBankAccountRequest.builder()
            .currency("USD")
            .build();

        when(session.getAttribute("username")).thenReturn("testuser");
    }

    @Nested
    @DisplayName("Create Bank Account Tests")
    class CreateBankAccountTests {

        @Test
        @DisplayName("Should successfully create bank account")
        void createBankAccount_Success() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doNothing().when(accountServiceClient).createBankAccount(any());

            // When
            String result = bankAccountController.createBankAccount(createRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(accountServiceClient).createBankAccount(createRequest);
            verify(redirectAttributes).addFlashAttribute("success", "Bank account created successfully");
            verify(redirectAttributes, never()).addFlashAttribute(eq("error"), anyString());
        }

        @Test
        @DisplayName("Should handle validation error for missing currency")
        void createBankAccount_ValidationError_MissingCurrency() {
            // Given
            FieldError error = new FieldError("createRequest", "currency", "Currency is required");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = bankAccountController.createBankAccount(createRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Currency is required");
            verify(accountServiceClient, never()).createBankAccount(any());
        }

        @Test
        @DisplayName("Should handle validation error for invalid currency")
        void createBankAccount_ValidationError_InvalidCurrency() {
            // Given
            CreateBankAccountRequest invalidRequest = CreateBankAccountRequest.builder()
                .currency("EUR")
                .build();
            FieldError error = new FieldError("createRequest", "currency", "Currency must be RUB, USD, or CNY");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = bankAccountController.createBankAccount(invalidRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Currency must be RUB, USD, or CNY"));
            verify(accountServiceClient, never()).createBankAccount(any());
        }

        @Test
        @DisplayName("Should handle service exception with user-friendly message")
        void createBankAccount_ServiceException() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Database connection failed"))
                .when(accountServiceClient).createBankAccount(any());

            // When
            String result = bankAccountController.createBankAccount(createRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Failed to create bank account. Please try again.");
        }

        @Test
        @DisplayName("Should filter technical error details")
        void createBankAccount_FiltersTechnicalErrors() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("{\"status\":500,\"error\":\"Internal Server Error\"}"))
                .when(accountServiceClient).createBankAccount(any());

            // When
            String result = bankAccountController.createBankAccount(createRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Failed to create bank account. Please try again.");
        }

        @Test
        @DisplayName("Should preserve user-friendly error messages")
        void createBankAccount_PreservesUserFriendlyErrors() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Currency already exists"))
                .when(accountServiceClient).createBankAccount(any());

            // When
            String result = bankAccountController.createBankAccount(createRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Currency already exists");
        }

        @Test
        @DisplayName("Should create account with RUB currency")
        void createBankAccount_RubCurrency() {
            // Given
            CreateBankAccountRequest rubRequest = CreateBankAccountRequest.builder()
                .currency("RUB")
                .build();
            when(bindingResult.hasErrors()).thenReturn(false);
            doNothing().when(accountServiceClient).createBankAccount(any());

            // When
            String result = bankAccountController.createBankAccount(rubRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(accountServiceClient).createBankAccount(rubRequest);
            verify(redirectAttributes).addFlashAttribute("success", "Bank account created successfully");
        }

        @Test
        @DisplayName("Should create account with CNY currency")
        void createBankAccount_CnyCurrency() {
            // Given
            CreateBankAccountRequest cnyRequest = CreateBankAccountRequest.builder()
                .currency("CNY")
                .build();
            when(bindingResult.hasErrors()).thenReturn(false);
            doNothing().when(accountServiceClient).createBankAccount(any());

            // When
            String result = bankAccountController.createBankAccount(cnyRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(accountServiceClient).createBankAccount(cnyRequest);
        }
    }

    @Nested
    @DisplayName("Delete Bank Account Tests")
    class DeleteBankAccountTests {

        @Test
        @DisplayName("Should successfully delete bank account")
        void deleteBankAccount_Success() {
            // Given
            Long bankAccountId = 123L;
            doNothing().when(accountServiceClient).deleteBankAccount(bankAccountId);

            // When
            String result = bankAccountController.deleteBankAccount(bankAccountId, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(accountServiceClient).deleteBankAccount(bankAccountId);
            verify(redirectAttributes).addFlashAttribute("success", "Bank account deleted successfully");
            verify(redirectAttributes, never()).addFlashAttribute(eq("error"), anyString());
        }

        @Test
        @DisplayName("Should handle invalid bank account ID (null)")
        void deleteBankAccount_InvalidId_Null() {
            // Given
            Long bankAccountId = null;

            // When
            String result = bankAccountController.deleteBankAccount(bankAccountId, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Invalid bank account ID");
            verify(accountServiceClient, never()).deleteBankAccount(any());
        }

        @Test
        @DisplayName("Should handle invalid bank account ID (negative)")
        void deleteBankAccount_InvalidId_Negative() {
            // Given
            Long bankAccountId = -1L;

            // When
            String result = bankAccountController.deleteBankAccount(bankAccountId, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Invalid bank account ID");
            verify(accountServiceClient, never()).deleteBankAccount(any());
        }

        @Test
        @DisplayName("Should handle invalid bank account ID (zero)")
        void deleteBankAccount_InvalidId_Zero() {
            // Given
            Long bankAccountId = 0L;

            // When
            String result = bankAccountController.deleteBankAccount(bankAccountId, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Invalid bank account ID");
            verify(accountServiceClient, never()).deleteBankAccount(any());
        }

        @Test
        @DisplayName("Should handle service exception with user-friendly message")
        void deleteBankAccount_ServiceException() {
            // Given
            Long bankAccountId = 123L;
            doThrow(new RuntimeException("Database error"))
                .when(accountServiceClient).deleteBankAccount(bankAccountId);

            // When
            String result = bankAccountController.deleteBankAccount(bankAccountId, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Failed to delete bank account. Please try again.");
        }

        @Test
        @DisplayName("Should preserve user-friendly error messages")
        void deleteBankAccount_PreservesUserFriendlyErrors() {
            // Given
            Long bankAccountId = 123L;
            doThrow(new RuntimeException("Account not found"))
                .when(accountServiceClient).deleteBankAccount(bankAccountId);

            // When
            String result = bankAccountController.deleteBankAccount(bankAccountId, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Account not found");
        }

        @Test
        @DisplayName("Should log username during deletion")
        void deleteBankAccount_LogsUsername() {
            // Given
            Long bankAccountId = 123L;
            when(session.getAttribute("username")).thenReturn("testuser");
            doNothing().when(accountServiceClient).deleteBankAccount(bankAccountId);

            // When
            bankAccountController.deleteBankAccount(bankAccountId, session, redirectAttributes);

            // Then
            verify(session).getAttribute("username");
        }
    }
}

