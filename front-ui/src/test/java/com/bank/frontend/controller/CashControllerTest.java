package com.bank.frontend.controller;

import com.bank.common.dto.contracts.cash.CashOperationRequest;
import com.bank.frontend.service.CashServiceClient;
import com.bank.frontend.service.LocalizationService;
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
import org.springframework.validation.SmartValidator;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CashController Tests")
class CashControllerTest {

    @Mock
    private CashServiceClient cashServiceClient;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private HttpSession session;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @Mock
    private SmartValidator validator;

    @InjectMocks
    private CashController cashController;

    private CashOperationRequest depositRequest;
    private CashOperationRequest withdrawRequest;

    @BeforeEach
    void setUp() {
        depositRequest = CashOperationRequest.builder()
            .bankAccountId(123L)
            .amount(100.0)
            .type("DEPOSIT")
            .build();

        withdrawRequest = CashOperationRequest.builder()
            .bankAccountId(123L)
            .amount(50.0)
            .type("WITHDRAWAL")
            .build();

        when(session.getAttribute("username")).thenReturn("testuser");
        lenient().when(localizationService.resolveMessage(any()))
            .thenAnswer(invocation -> {
                Object error = invocation.getArgument(0);
                if (error instanceof org.springframework.validation.ObjectError objectError) {
                    return objectError.getDefaultMessage();
                }
                return "";
            });
        lenient().when(localizationService.getMessage(eq("cash.depositSuccess"), any(Object[].class)))
            .thenReturn("Deposit completed successfully");
        lenient().when(localizationService.getMessage(eq("cash.depositError"), any(Object[].class)))
            .thenReturn("Deposit failed. Please try again.");
        lenient().when(localizationService.getMessage(eq("cash.withdrawSuccess"), any(Object[].class)))
            .thenReturn("Withdrawal completed successfully");
        lenient().when(localizationService.getMessage(eq("cash.withdrawError"), any(Object[].class)))
            .thenReturn("Withdrawal failed. Please try again.");
        doAnswer(invocation -> null).when(validator).validate(any(), any());
    }

    @Nested
    @DisplayName("Deposit Tests")
    class DepositTests {

        @Test
        @DisplayName("Should successfully process deposit")
        void deposit_Success() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doNothing().when(cashServiceClient).deposit(any());

            // When
            String result = cashController.deposit(depositRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(cashServiceClient).deposit(any(CashOperationRequest.class));
            verify(redirectAttributes).addFlashAttribute("success", "Deposit completed successfully");
            verify(redirectAttributes, never()).addFlashAttribute(eq("error"), anyString());
        }

        @Test
        @DisplayName("Should handle validation error for missing bank account ID")
        void deposit_ValidationError_MissingBankAccountId() {
            // Given
            FieldError error = new FieldError("cashOperationRequest", "bankAccountId", "Bank account ID is required");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = cashController.deposit(depositRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Bank account ID is required");
            verify(cashServiceClient, never()).deposit(any());
        }

        @Test
        @DisplayName("Should handle validation error for missing amount")
        void deposit_ValidationError_MissingAmount() {
            // Given
            FieldError error = new FieldError("cashOperationRequest", "amount", "Amount is required");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = cashController.deposit(depositRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Amount is required");
        }

        @Test
        @DisplayName("Should handle validation error for negative amount")
        void deposit_ValidationError_NegativeAmount() {
            // Given
            FieldError error = new FieldError("cashOperationRequest", "amount", "Amount must be positive");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = cashController.deposit(depositRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Amount must be positive"));
        }

        @Test
        @DisplayName("Should handle service exception with user-friendly message")
        void deposit_ServiceException() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Database error"))
                .when(cashServiceClient).deposit(any());

            // When
            String result = cashController.deposit(depositRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Deposit failed. Please try again.");
        }

        @Test
        @DisplayName("Should filter technical error details")
        void deposit_FiltersTechnicalErrors() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("{\"status\":500,\"error\":\"Internal Server Error\"}"))
                .when(cashServiceClient).deposit(any());

            // When
            String result = cashController.deposit(depositRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Deposit failed. Please try again.");
        }

        @Test
        @DisplayName("Should preserve user-friendly error messages")
        void deposit_PreservesUserFriendlyErrors() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Account not found"))
                .when(cashServiceClient).deposit(any());

            // When
            String result = cashController.deposit(depositRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Account not found");
        }

        @Test
        @DisplayName("Should set operation type to DEPOSIT")
        void deposit_SetsOperationType() {
            // Given
            CashOperationRequest request = CashOperationRequest.builder()
                .bankAccountId(123L)
                .amount(100.0)
                .build();
            when(bindingResult.hasErrors()).thenReturn(false);
            doNothing().when(cashServiceClient).deposit(any());

            // When
            cashController.deposit(request, bindingResult, session, redirectAttributes);

            // Then
            assertThat(request.getType()).isEqualTo("DEPOSIT");
        }
    }

    @Nested
    @DisplayName("Withdrawal Tests")
    class WithdrawalTests {

        @Test
        @DisplayName("Should successfully process withdrawal")
        void withdraw_Success() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doNothing().when(cashServiceClient).withdraw(any());

            // When
            String result = cashController.withdraw(withdrawRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(cashServiceClient).withdraw(any(CashOperationRequest.class));
            verify(redirectAttributes).addFlashAttribute("success", "Withdrawal completed successfully");
            verify(redirectAttributes, never()).addFlashAttribute(eq("error"), anyString());
        }

        @Test
        @DisplayName("Should handle validation error for missing bank account ID")
        void withdraw_ValidationError_MissingBankAccountId() {
            // Given
            FieldError error = new FieldError("cashOperationRequest", "bankAccountId", "Bank account ID is required");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = cashController.withdraw(withdrawRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Bank account ID is required");
            verify(cashServiceClient, never()).withdraw(any());
        }

        @Test
        @DisplayName("Should handle validation error for negative amount")
        void withdraw_ValidationError_NegativeAmount() {
            // Given
            FieldError error = new FieldError("cashOperationRequest", "amount", "Amount must be positive");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = cashController.withdraw(withdrawRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Amount must be positive"));
        }

        @Test
        @DisplayName("Should handle service exception with user-friendly message")
        void withdraw_ServiceException() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Database error"))
                .when(cashServiceClient).withdraw(any());

            // When
            String result = cashController.withdraw(withdrawRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Withdrawal failed. Please try again.");
        }

        @Test
        @DisplayName("Should preserve user-friendly error messages for insufficient funds")
        void withdraw_PreservesInsufficientFundsError() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Insufficient funds"))
                .when(cashServiceClient).withdraw(any());

            // When
            String result = cashController.withdraw(withdrawRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Insufficient funds");
        }

        @Test
        @DisplayName("Should set operation type to WITHDRAWAL")
        void withdraw_SetsOperationType() {
            // Given
            CashOperationRequest request = CashOperationRequest.builder()
                .bankAccountId(123L)
                .amount(50.0)
                .build();
            when(bindingResult.hasErrors()).thenReturn(false);
            doNothing().when(cashServiceClient).withdraw(any());

            // When
            cashController.withdraw(request, bindingResult, session, redirectAttributes);

            // Then
            assertThat(request.getType()).isEqualTo("WITHDRAWAL");
        }

        @Test
        @DisplayName("Should handle multiple validation errors")
        void withdraw_MultipleValidationErrors() {
            // Given
            FieldError error1 = new FieldError("cashOperationRequest", "bankAccountId", "Bank account ID is required");
            FieldError error2 = new FieldError("cashOperationRequest", "amount", "Amount is required");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error1, error2));

            // When
            String result = cashController.withdraw(withdrawRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Bank account ID is required"));
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Amount is required"));
        }
    }
}
