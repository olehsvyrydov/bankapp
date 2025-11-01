package com.bank.frontend.controller;

import com.bank.common.dto.contracts.accounts.UpdateAccountRequest;
import com.bank.frontend.service.AccountServiceClient;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController Tests")
class AccountControllerTest {

    @Mock
    private AccountServiceClient accountServiceClient;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private HttpSession session;

    @Mock
    private BindingResult bindingResult;

    @Mock
    private RedirectAttributes redirectAttributes;

    @InjectMocks
    private AccountController accountController;

    private UpdateAccountRequest updateRequest;

    @BeforeEach
    void setUp() {
        updateRequest = UpdateAccountRequest.builder()
            .firstName("John")
            .lastName("Doe")
            .email("john.doe@example.com")
            .birthDate(LocalDate.of(1990, 1, 1))
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

        lenient().when(localizationService.getMessage(eq("account.update.success"), any(Object[].class)))
            .thenReturn("Account updated successfully");
        lenient().when(localizationService.getMessage(eq("account.update.error"), any(Object[].class)))
            .thenReturn("Failed to update account. Please try again.");
    }

    @Nested
    @DisplayName("Update Account Tests")
    class UpdateAccountTests {

        @Test
        @DisplayName("Should successfully update account")
        void updateAccount_Success() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doNothing().when(accountServiceClient).updateAccount(any());

            // When
            String result = accountController.updateAccount(updateRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(accountServiceClient).updateAccount(updateRequest);
            verify(redirectAttributes).addFlashAttribute("success", "Account updated successfully");
            verify(redirectAttributes, never()).addFlashAttribute(eq("error"), anyString());
        }

        @Test
        @DisplayName("Should return error when validation fails")
        void updateAccount_ValidationError() {
            // Given
            FieldError fieldError = new FieldError("updateRequest", "firstName", "First name is required");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError));

            // When
            String result = accountController.updateAccount(updateRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "First name is required");
            verify(accountServiceClient, never()).updateAccount(any());
        }

        @Test
        @DisplayName("Should handle multiple validation errors")
        void updateAccount_MultipleValidationErrors() {
            // Given
            FieldError error1 = new FieldError("updateRequest", "firstName", "First name is required");
            FieldError error2 = new FieldError("updateRequest", "email", "Invalid email format");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error1, error2));

            // When
            String result = accountController.updateAccount(updateRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("First name is required"));
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Invalid email format"));
        }

        @Test
        @DisplayName("Should handle service exception with user-friendly message")
        void updateAccount_ServiceException() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Database connection failed"))
                .when(accountServiceClient).updateAccount(any());

            // When
            String result = accountController.updateAccount(updateRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Database connection failed");
            verify(redirectAttributes, never()).addFlashAttribute(eq("success"), anyString());
        }

        @Test
        @DisplayName("Should filter technical error details")
        void updateAccount_FiltersTechnicalErrors() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("{\"status\":500,\"error\":\"Internal Server Error\"}"))
                .when(accountServiceClient).updateAccount(any());

            // When
            String result = accountController.updateAccount(updateRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Internal Server Error");
        }

        @Test
        @DisplayName("Should preserve user-friendly error messages")
        void updateAccount_PreservesUserFriendlyErrors() {
            // Given
            when(bindingResult.hasErrors()).thenReturn(false);
            doThrow(new RuntimeException("Email already exists"))
                .when(accountServiceClient).updateAccount(any());

            // When
            String result = accountController.updateAccount(updateRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Email already exists");
        }

        @Test
        @DisplayName("Should validate first name length")
        void updateAccount_FirstNameTooShort() {
            // Given
            UpdateAccountRequest shortNameRequest = UpdateAccountRequest.builder()
                .firstName("J")
                .lastName("Doe")
                .email("john@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();

            FieldError error = new FieldError("updateRequest", "firstName",
                "First name must be between 2 and 50 characters");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = accountController.updateAccount(shortNameRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"),
                contains("First name must be between 2 and 50 characters"));
        }

        @Test
        @DisplayName("Should validate email format")
        void updateAccount_InvalidEmail() {
            // Given
            FieldError error = new FieldError("updateRequest", "email", "Invalid email format");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = accountController.updateAccount(updateRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Invalid email format"));
        }

        @Test
        @DisplayName("Should validate birth date is in the past")
        void updateAccount_BirthDateInFuture() {
            // Given
            FieldError error = new FieldError("updateRequest", "birthDate", "Birth date must be in the past");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = accountController.updateAccount(updateRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Birth date must be in the past"));
        }

        @Test
        @DisplayName("Should validate minimum age (must be at least 18 years old)")
        void updateAccount_AgeTooYoung() {
            // Given
            UpdateAccountRequest youngRequest = UpdateAccountRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .birthDate(LocalDate.now().minusYears(17)) // 17 years old
                .build();

            FieldError error = new FieldError("updateRequest", "birthDate", "Must be at least 18 years old");
            when(bindingResult.hasErrors()).thenReturn(true);
            when(bindingResult.getAllErrors()).thenReturn(List.of(error));

            // When
            String result = accountController.updateAccount(youngRequest, bindingResult, session, redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute(eq("error"), contains("Must be at least 18 years old"));
        }

        @Test
        @DisplayName("Should log username during update")
        void updateAccount_LogsUsername() {
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

        lenient().when(localizationService.getMessage(eq("account.update.success"), any(Object[].class)))
            .thenReturn("Account updated successfully");
        lenient().when(localizationService.getMessage(eq("account.update.error"), any(Object[].class)))
            .thenReturn("Failed to update account. Please try again.");
            doNothing().when(accountServiceClient).updateAccount(any());

            // When
            accountController.updateAccount(updateRequest, bindingResult, session, redirectAttributes);

            // Then
            verify(session).getAttribute("username");
        }
    }
}

