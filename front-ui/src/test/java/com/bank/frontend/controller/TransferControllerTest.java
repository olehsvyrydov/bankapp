package com.bank.frontend.controller;

import com.bank.common.dto.contracts.transfer.TransferRequest;
import com.bank.frontend.service.TransferServiceClient;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransferController Tests")
class TransferControllerTest {

    @Mock
    private TransferServiceClient transferServiceClient;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private HttpSession session;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private TransferController transferController;

    private TransferRequest transferRequest;

    @BeforeEach
    void setUp() {
        transferRequest = TransferRequest.builder()
            .fromBankAccountId(123L)
            .toBankAccountId(456L)
            .amount(100.0)
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
        lenient().when(localizationService.getMessage(eq("transfer.sameAccount"), any(Object[].class)))
            .thenReturn("Cannot transfer to the same account");
        lenient().when(localizationService.getMessage(eq("transfer.success"), any(Object[].class)))
            .thenReturn("Transfer completed successfully");
        lenient().when(localizationService.getMessage(eq("transfer.error"), any(Object[].class)))
            .thenReturn("Transfer failed. Please try again.");
    }

    @Nested
    @DisplayName("Transfer Own Tests")
    class TransferOwnTests {

        @Test
        @DisplayName("Should successfully transfer between own accounts")
        void transferOwn_Success() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doNothing().when(transferServiceClient).transfer(any());

            // When
            String result = transferController.transferOwn(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(transferServiceClient).transfer(transferRequest);
            verify(redirectAttributes).addFlashAttribute("success", "Transfer completed successfully");
            verify(redirectAttributes, never()).addFlashAttribute(eq("error"), anyString());
        }

        @Test
        @DisplayName("Should reject transfer to same account")
        void transferOwn_SameAccount() {
            // Given
            TransferRequest sameAccountRequest = TransferRequest.builder()
                .fromBankAccountId(123L)
                .toBankAccountId(123L)
                .amount(100.0)
                .build();
            when(bindingResult.hasErrors()).thenReturn(false);

            // When
            String result = transferController.transferOwn(sameAccountRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Cannot transfer to the same account");
            verify(transferServiceClient, never()).transfer(any());
        }

        @Test
        @DisplayName("Should handle validation error for missing from account")
        void transferOwn_ValidationError_MissingFromAccount() {
            // Given
            FieldError error = new FieldError("transferRequest", "fromBankAccountId", "From bank account ID is required");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = transferController.transferOwn(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "From bank account ID is required");
            verify(transferServiceClient, never()).transfer(any());
        }

        @Test
        @DisplayName("Should handle validation error for missing to account")
        void transferOwn_ValidationError_MissingToAccount() {
            // Given
            FieldError error = new FieldError("transferRequest", "toBankAccountId", "To bank account ID is required");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = transferController.transferOwn(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "To bank account ID is required");
        }

        @Test
        @DisplayName("Should handle validation error for negative amount")
        void transferOwn_ValidationError_NegativeAmount() {
            // Given
            FieldError error = new FieldError("transferRequest", "amount", "Amount must be positive");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = transferController.transferOwn(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Amount must be positive"));
        }

        @Test
        @DisplayName("Should handle service exception with user-friendly message")
        void transferOwn_ServiceException() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Database error"))
                .when(transferServiceClient).transfer(any());

            // When
            String result = transferController.transferOwn(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Transfer failed. Please try again.");
        }

        @Test
        @DisplayName("Should filter technical error details")
        void transferOwn_FiltersTechnicalErrors() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("{\"status\":500,\"error\":\"Internal Server Error\"}"))
                .when(transferServiceClient).transfer(any());

            // When
            String result = transferController.transferOwn(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Transfer failed. Please try again.");
        }

        @Test
        @DisplayName("Should preserve user-friendly error messages")
        void transferOwn_PreservesUserFriendlyErrors() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Insufficient funds"))
                .when(transferServiceClient).transfer(any());

            // When
            String result = transferController.transferOwn(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Insufficient funds");
        }

        @Test
        @DisplayName("Should handle multiple validation errors")
        void transferOwn_MultipleValidationErrors() {
            // Given
            FieldError error1 = new FieldError("transferRequest", "fromBankAccountId", "From bank account ID is required");
            FieldError error2 = new FieldError("transferRequest", "amount", "Amount is required");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error1, error2));

            // When
            String result = transferController.transferOwn(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("From bank account ID is required"));
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Amount is required"));
        }
    }

    @Nested
    @DisplayName("Transfer Other Tests")
    class TransferOtherTests {

        @Test
        @DisplayName("Should successfully transfer to another user's account")
        void transferOther_Success() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doNothing().when(transferServiceClient).transfer(any());

            // When
            String result = transferController.transferOther(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(transferServiceClient).transfer(transferRequest);
            verify(redirectAttributes).addFlashAttribute("success", "Transfer completed successfully");
            verify(redirectAttributes, never()).addFlashAttribute(eq("error"), anyString());
        }

        @Test
        @DisplayName("Should reject transfer to same account")
        void transferOther_SameAccount() {
            // Given
            TransferRequest sameAccountRequest = TransferRequest.builder()
                .fromBankAccountId(123L)
                .toBankAccountId(123L)
                .amount(100.0)
                .build();
            when(bindingResult.hasErrors()).thenReturn(false);

            // When
            String result = transferController.transferOther(sameAccountRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Cannot transfer to the same account");
            verify(transferServiceClient, never()).transfer(any());
        }

        @Test
        @DisplayName("Should handle validation errors")
        void transferOther_ValidationError() {
            // Given
            FieldError error = new FieldError("transferRequest", "amount", "Amount is required");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = transferController.transferOther(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Amount is required");
            verify(transferServiceClient, never()).transfer(any());
        }

        @Test
        @DisplayName("Should handle service exception with user-friendly message")
        void transferOther_ServiceException() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Service unavailable"))
                .when(transferServiceClient).transfer(any());

            // When
            String result = transferController.transferOther(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Transfer failed. Please try again.");
        }

        @Test
        @DisplayName("Should preserve user-friendly error messages")
        void transferOther_PreservesUserFriendlyErrors() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Account not found"))
                .when(transferServiceClient).transfer(any());

            // When
            String result = transferController.transferOther(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Account not found");
        }

        @Test
        @DisplayName("Should log username during transfer")
        void transferOther_LogsUsername() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            when(session.getAttribute("username")).thenReturn("testuser");
        lenient().when(localizationService.resolveMessage(any()))
            .thenAnswer(invocation -> {
                Object error = invocation.getArgument(0);
                if (error instanceof org.springframework.validation.ObjectError objectError) {
                    return objectError.getDefaultMessage();
                }
                return "";
            });
        lenient().when(localizationService.getMessage(eq("transfer.sameAccount"), any(Object[].class)))
            .thenReturn("Cannot transfer to the same account");
        lenient().when(localizationService.getMessage(eq("transfer.success"), any(Object[].class)))
            .thenReturn("Transfer completed successfully");
        lenient().when(localizationService.getMessage(eq("transfer.error"), any(Object[].class)))
            .thenReturn("Transfer failed. Please try again.");
            doNothing().when(transferServiceClient).transfer(any());

            // When
            transferController.transferOther(transferRequest, bindingResult, session, redirectAttributes);

            // Then
            verify(session).getAttribute("username");
        }
    }
}

