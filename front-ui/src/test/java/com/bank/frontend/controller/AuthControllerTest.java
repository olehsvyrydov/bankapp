package com.bank.frontend.controller;

import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.AccountDTO;
import com.bank.common.dto.contracts.accounts.ChangePasswordRequest;
import com.bank.common.dto.contracts.accounts.CreateAccountRequest;
import com.bank.common.dto.contracts.auth.LoginRequest;
import com.bank.common.dto.contracts.auth.RegisterRequest;
import com.bank.common.dto.contracts.auth.TokenResponse;
import com.bank.common.exception.BusinessException;
import com.bank.frontend.client.AccountsClient;
import com.bank.frontend.service.LocalizationService;
import com.bank.frontend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Tests")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AccountsClient accountsClient;

    @Mock
    private LocalizationService localizationService;

    @Mock
    private HttpSession session;

    @Mock
    private Model model;

    @InjectMocks
    private AuthController authController;

    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        stubMessage("auth.invalidCredentials", "Invalid username or password");
        stubMessage("auth.service.unavailable", "Authentication service unavailable. Please try again later.");
        stubMessage("auth.logout.success", "You have been logged out successfully");
        stubMessage("auth.session.expired", "Your session has expired. Please log in again.");
        stubMessage("register.success.login", "Registration successful. Please log in.");
        stubMessage("message.passwordMismatch", "Passwords do not match");
        stubMessage("message.passwordTooShort", "Password must be at least 6 characters");
        stubMessage("register.account.create.failure", "Unable to create account. Please try again later.");
        stubMessage("register.service.unavailable", "Registration service unavailable. Please try again later.");
        stubMessage("register.error.details", "Registration failed. Please check your details and try again.");
        stubMessage("register.error.tryAgain", "Registration failed. Please try again.");
        stubMessage("auth.password.change.success", "Password changed successfully");
        stubMessage("auth.password.change.error", "Failed to change password. Please try again.");
    }

    private void stubMessage(String key, String value) {
        lenient().when(localizationService.getMessage(eq(key), any(Object[].class)))
            .thenReturn(value);
    }


    @Test
    @DisplayName("Should successfully login and redirect to home when credentials are valid")
    void performLogin_Success() {
        // Given
        TokenResponse tokenResponse = new TokenResponse(
            "access-token-123",
            "refresh-token-456",
            "Bearer",
            3600L,
            "read write"
        );
        ApiResponse<TokenResponse> apiResponse = ApiResponse.success(tokenResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        String result = authController.performLogin(loginRequest, session, model);

        // Then
        assertThat(result).isEqualTo("redirect:/home");

        verify(session).setAttribute("access_token", "access-token-123");
        verify(session).setAttribute("refresh_token", "refresh-token-456");
        verify(session).setAttribute("token_scope", "read write");
        verify(session).setAttribute("token_type", "Bearer");
        verify(session).setAttribute("username", "testuser");
        verify(session).setMaxInactiveInterval(30 * 60);

        verify(model, never()).addAttribute(eq("error"), anyString());
    }

    @Test
    @DisplayName("Should return login page with error when API response indicates failure")
    void performLogin_ApiResponseFailure() {
        // Given
        ApiResponse<TokenResponse> apiResponse = ApiResponse.error("Invalid credentials");

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        String result = authController.performLogin(loginRequest, session, model);

        // Then
        assertThat(result).isEqualTo("login");

        verify(model).addAttribute("error", "Invalid username or password");
        verify(model).addAttribute("loginRequest", loginRequest);

        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    @DisplayName("Should return login page with error when token response is null")
    void performLogin_NullTokenResponse() {
        // Given
        ApiResponse<TokenResponse> apiResponse = ApiResponse.success(null);

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        String result = authController.performLogin(loginRequest, session, model);

        // Then
        assertThat(result).isEqualTo("login");

        verify(model).addAttribute("error", "Invalid username or password");
        verify(model).addAttribute("loginRequest", loginRequest);

        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    @DisplayName("Should return login page with error when access token is null")
    void performLogin_NullAccessToken() {
        // Given
        TokenResponse tokenResponse = new TokenResponse(
            null,
            "refresh-token-456",
            "Bearer",
            3600L,
            "read write"
        );
        ApiResponse<TokenResponse> apiResponse = ApiResponse.success(tokenResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        String result = authController.performLogin(loginRequest, session, model);

        // Then
        assertThat(result).isEqualTo("login");

        verify(model).addAttribute("error", "Invalid username or password");
        verify(model).addAttribute("loginRequest", loginRequest);

        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    @DisplayName("Should handle exception and return login page with service unavailable error")
    void performLogin_ExceptionThrown() {
        // Given
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new RuntimeException("Service unavailable"));

        // When
        String result = authController.performLogin(loginRequest, session, model);

        // Then
        assertThat(result).isEqualTo("login");

        verify(model).addAttribute("error", "Authentication service unavailable. Please try again later.");
        verify(model).addAttribute("loginRequest", loginRequest);

        verify(session, never()).setAttribute(anyString(), any());
    }

    @Test
    @DisplayName("Should set session timeout to 30 minutes on successful login")
    void performLogin_SessionTimeoutSet() {
        // Given
        TokenResponse tokenResponse = new TokenResponse(
            "access-token-123",
            "refresh-token-456",
            "Bearer",
            3600L,
            "read write"
        );
        ApiResponse<TokenResponse> apiResponse = ApiResponse.success(tokenResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        authController.performLogin(loginRequest, session, model);

        // Then
        verify(session).setMaxInactiveInterval(1800); // 30 minutes in seconds
    }

    @Test
    @DisplayName("Should store all token attributes in session on successful login")
    void performLogin_AllTokenAttributesStored() {
        // Given
        TokenResponse tokenResponse = new TokenResponse(
            "access-token-123",
            "refresh-token-456",
            "Bearer",
            3600L,
            "openid profile email"
        );
        ApiResponse<TokenResponse> apiResponse = ApiResponse.success(tokenResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        authController.performLogin(loginRequest, session, model);

        // Then
        verify(session).setAttribute("access_token", "access-token-123");
        verify(session).setAttribute("refresh_token", "refresh-token-456");
        verify(session).setAttribute("token_scope", "openid profile email");
        verify(session).setAttribute("token_type", "Bearer");
        verify(session).setAttribute("username", "testuser");
    }

    @Test
    @DisplayName("Should preserve login request in model on failure")
    void performLogin_LoginRequestPreservedOnFailure() {
        // Given
        ApiResponse<TokenResponse> apiResponse = ApiResponse.error("Invalid credentials");

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        authController.performLogin(loginRequest, session, model);

        // Then
        verify(model).addAttribute("loginRequest", loginRequest);
    }

    @Test
    @DisplayName("Should call authService.login with correct login request")
    void performLogin_AuthServiceCalledWithCorrectRequest() {
        // Given
        LoginRequest specificRequest = new LoginRequest();
        specificRequest.setUsername("admin");
        specificRequest.setPassword("admin123");
        specificRequest.setRememberMe(true);

        TokenResponse tokenResponse = new TokenResponse(
            "access-token-123",
            "refresh-token-456",
            "Bearer",
            3600L,
            "read write"
        );
        ApiResponse<TokenResponse> apiResponse = ApiResponse.success(tokenResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        authController.performLogin(specificRequest, session, model);

        // Then
        verify(authService).login(specificRequest);
    }

    @Test
    @DisplayName("Should handle network timeout exception gracefully")
    void performLogin_NetworkTimeoutException() {
        // Given
        when(authService.login(any(LoginRequest.class)))
            .thenThrow(new RuntimeException("Connection timeout"));

        // When
        String result = authController.performLogin(loginRequest, session, model);

        // Then
        assertThat(result).isEqualTo("login");

        verify(model).addAttribute("error", "Authentication service unavailable. Please try again later.");
    }

    @Test
    @DisplayName("Should return login view when response is successful but data is null")
    void performLogin_SuccessfulResponseButNullData() {
        // Given
        ApiResponse<TokenResponse> apiResponse = new ApiResponse<>();
        apiResponse.setSuccess(true);
        apiResponse.setData(null);

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        String result = authController.performLogin(loginRequest, session, model);

        // Then
        assertThat(result).isEqualTo("login");
        verify(model).addAttribute("error", "Invalid username or password");
    }

    @Test
    @DisplayName("Should handle empty username in login request")
    void performLogin_EmptyUsername() {
        // Given
        loginRequest.setUsername("");
        ApiResponse<TokenResponse> apiResponse = ApiResponse.error("Username is required");

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        String result = authController.performLogin(loginRequest, session, model);

        // Then
        assertThat(result).isEqualTo("login");
        verify(model).addAttribute("error", "Invalid username or password");
    }

    @Test
    @DisplayName("Should handle null refresh token in successful response")
    void performLogin_NullRefreshToken() {
        // Given
        TokenResponse tokenResponse = new TokenResponse(
            "access-token-123",
            null,
            "Bearer",
            3600L,
            "read write"
        );
        ApiResponse<TokenResponse> apiResponse = ApiResponse.success(tokenResponse);

        when(authService.login(any(LoginRequest.class))).thenReturn(apiResponse);

        // When
        String result = authController.performLogin(loginRequest, session, model);

        // Then
        assertThat(result).isEqualTo("redirect:/home");

        verify(session).setAttribute("access_token", "access-token-123");
        verify(session).setAttribute("refresh_token", null);
    }

    // ========== INDEX METHOD TESTS ==========

    @Nested
    @DisplayName("Index Method Tests")
    class IndexTests {

        @Test
        @DisplayName("Should redirect to home when user has active session")
        void index_WithActiveSession() {
            // Given
            when(session.getAttribute("access_token")).thenReturn("valid-token");

            // When
            String result = authController.index(session);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(session).getAttribute("access_token");
        }

        @Test
        @DisplayName("Should redirect to login when user has no active session")
        void index_WithoutActiveSession() {
            // Given
            when(session.getAttribute("access_token")).thenReturn(null);

            // When
            String result = authController.index(session);

            // Then
            assertThat(result).isEqualTo("redirect:/login");
            verify(session).getAttribute("access_token");
        }
    }

    // ========== LOGIN PAGE METHOD TESTS ==========

    @Nested
    @DisplayName("LoginPage Method Tests")
    class LoginPageTests {

        @Test
        @DisplayName("Should redirect to home when user already logged in")
        void loginPage_WithActiveSession() {
            // Given
            when(session.getAttribute("access_token")).thenReturn("valid-token");

            // When
            String result = authController.loginPage(null, null, null, null, session, model);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(model, never()).addAttribute(anyString(), any());
        }

        @Test
        @DisplayName("Should show login page with error message when error param present")
        void loginPage_WithErrorParam() {
            // Given
            when(session.getAttribute("access_token")).thenReturn(null);

            // When
            String result = authController.loginPage("true", null, null, null, session, model);

            // Then
            assertThat(result).isEqualTo("login");
            verify(model).addAttribute("error", "Invalid username or password");
            verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should show login page with logout message when logout param present")
        void loginPage_WithLogoutParam() {
            // Given
            when(session.getAttribute("access_token")).thenReturn(null);

            // When
            String result = authController.loginPage(null, "true", null, null, session, model);

            // Then
            assertThat(result).isEqualTo("login");
            verify(model).addAttribute("message", "You have been logged out successfully");
            verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should show login page with session expired message")
        void loginPage_WithSessionExpiredParam() {
            // Given
            when(session.getAttribute("access_token")).thenReturn(null);

            // When
            String result = authController.loginPage(null, null, "true", null, session, model);

            // Then
            assertThat(result).isEqualTo("login");
            verify(model).addAttribute("error", "Your session has expired. Please log in again.");
            verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should show login page with registration success message")
        void loginPage_WithRegisteredParam() {
            // Given
            when(session.getAttribute("access_token")).thenReturn(null);

            // When
            String result = authController.loginPage(null, null, null, "true", session, model);

            // Then
            assertThat(result).isEqualTo("login");
            verify(model).addAttribute("message", "Registration successful. Please log in.");
            verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should show plain login page when no params present")
        void loginPage_WithNoParams() {
            // Given
            when(session.getAttribute("access_token")).thenReturn(null);

            // When
            String result = authController.loginPage(null, null, null, null, session, model);

            // Then
            assertThat(result).isEqualTo("login");
            verify(model).addAttribute(eq("loginRequest"), any(LoginRequest.class));
            verify(model, never()).addAttribute(eq("error"), anyString());
            verify(model, never()).addAttribute(eq("message"), anyString());
        }
    }

    // ========== REGISTER PAGE METHOD TESTS ==========

    @Nested
    @DisplayName("RegisterPage Method Tests")
    class RegisterPageTests {

        @Test
        @DisplayName("Should redirect to home when user already logged in")
        void registerPage_WithActiveSession() {
            // Given
            when(session.getAttribute("access_token")).thenReturn("valid-token");

            // When
            String result = authController.registerPage(session, model);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(model, never()).addAttribute(anyString(), any());
        }

        @Test
        @DisplayName("Should show register page when no active session")
        void registerPage_WithoutActiveSession() {
            // Given
            when(session.getAttribute("access_token")).thenReturn(null);

            // When
            String result = authController.registerPage(session, model);

            // Then
            assertThat(result).isEqualTo("register");
            verify(model).addAttribute(eq("registerRequest"), any(RegisterRequest.class));
        }
    }

    // ========== PERFORM REGISTER METHOD TESTS ==========

    @Nested
    @DisplayName("PerformRegister Method Tests")
    class PerformRegisterTests {

        private RegisterRequest registerRequest;

        @BeforeEach
        void setUp() {
            registerRequest = new RegisterRequest(
                "testuser",
                "Test",
                "User",
                "password123",
                "password123",
                "test@example.com",
                LocalDate.of(1990, 1, 1)
            );
        }

        @Test
        @DisplayName("Should successfully register user and redirect to login")
        void performRegister_Success() {
            // Given
            ApiResponse<Void> authResponse = ApiResponse.success(null);
            AccountDTO accountDTO = AccountDTO.builder()
                .id(1L)
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .birthDate(LocalDate.of(1990, 1, 1))
                .build();
            ApiResponse<AccountDTO> accountResponse = ApiResponse.success(accountDTO);

            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);
            when(accountsClient.createUserAccount(any(CreateAccountRequest.class))).thenReturn(accountResponse);

            // When
            String result = authController.performRegister(registerRequest, session, model);

            // Then
            assertThat(result).isEqualTo("redirect:/login?registered=true");
            verify(authService).register(registerRequest);
            verify(accountsClient).createUserAccount(any(CreateAccountRequest.class));
            verify(model, never()).addAttribute(eq("error"), anyString());
        }

        @Test
        @DisplayName("Should return error when passwords do not match")
        void performRegister_PasswordsDoNotMatch() {
            // Given
            RegisterRequest mismatchRequest = new RegisterRequest(
                "testuser",
                "Test",
                "User",
                "password123",
                "different",
                "test@example.com",
                LocalDate.of(1990, 1, 1)
            );

            // When
            String result = authController.performRegister(mismatchRequest, session, model);

            // Then
            assertThat(result).isEqualTo("register");
            verify(model).addAttribute("error", "Passwords do not match");
            verify(model).addAttribute("registerRequest", mismatchRequest);
            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return error when password is too short")
        void performRegister_PasswordTooShort() {
            // Given
            RegisterRequest shortPasswordRequest = new RegisterRequest(
                "testuser",
                "Test",
                "User",
                "123",
                "123",
                "test@example.com",
                LocalDate.of(1990, 1, 1)
            );

            // When
            String result = authController.performRegister(shortPasswordRequest, session, model);

            // Then
            assertThat(result).isEqualTo("register");
            verify(model).addAttribute("error", "Password must be at least 6 characters");
            verify(model).addAttribute("registerRequest", shortPasswordRequest);
            verify(authService, never()).register(any());
        }

        @Test
        @DisplayName("Should return user-friendly error when registration fails with simple message")
        void performRegister_RegistrationFailsWithSimpleMessage() {
            // Given
            ApiResponse<Void> authResponse = ApiResponse.error("Username already exists");
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            // When
            String result = authController.performRegister(registerRequest, session, model);

            // Then
            assertThat(result).isEqualTo("register");
            verify(model).addAttribute("error", "Username already exists");
            verify(model).addAttribute("registerRequest", registerRequest);
        }

        @Test
        @DisplayName("Should return default error when registration fails with JSON error")
        void performRegister_RegistrationFailsWithJsonError() {
            // Given
            String jsonError = "{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"Database connection failed\"}";
            ApiResponse<Void> authResponse = ApiResponse.error(jsonError);
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            // When
            String result = authController.performRegister(registerRequest, session, model);

            // Then
            assertThat(result).isEqualTo("register");
            verify(model).addAttribute("error", "Registration failed. Please check your details and try again.");
            verify(model).addAttribute("registerRequest", registerRequest);
        }

        @Test
        @DisplayName("Should return default error when registration fails with exception stacktrace")
        void performRegister_RegistrationFailsWithException() {
            // Given
            String exceptionMessage = "java.lang.RuntimeException: Connection timeout at line 123";
            ApiResponse<Void> authResponse = ApiResponse.error(exceptionMessage);
            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

            // When
            String result = authController.performRegister(registerRequest, session, model);

            // Then
            assertThat(result).isEqualTo("register");
            verify(model).addAttribute("error", "Registration failed. Please check your details and try again.");
            verify(model).addAttribute("registerRequest", registerRequest);
        }

        @Test
        @DisplayName("Should return error when account creation fails")
        void performRegister_AccountCreationFails() {
            // Given
            ApiResponse<Void> authResponse = ApiResponse.success(null);
            ApiResponse<AccountDTO> accountResponse = ApiResponse.error("Database error occurred");

            when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);
            when(accountsClient.createUserAccount(any(CreateAccountRequest.class))).thenReturn(accountResponse);

            // When
            String result = authController.performRegister(registerRequest, session, model);

            // Then
            assertThat(result).isEqualTo("register");
            verify(model).addAttribute("error", "Unable to create account. Please try again later.");
            verify(model).addAttribute("registerRequest", registerRequest);
        }

        @Test
        @DisplayName("Should handle exception and return service unavailable error")
        void performRegister_ExceptionThrown() {
            // Given
            when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Service down"));

            // When
            String result = authController.performRegister(registerRequest, session, model);

            // Then
            assertThat(result).isEqualTo("register");
            verify(model).addAttribute("error", "Registration service unavailable. Please try again later.");
            verify(model).addAttribute("registerRequest", registerRequest);
        }
    }

    // ========== LOGOUT METHOD TESTS ==========

    @Nested
    @DisplayName("Logout Method Tests")
    class LogoutTests {

        @Test
        @DisplayName("Should invalidate session and redirect to login with logout message")
        void logout_WithUsername() {
            // Given
            when(session.getAttribute("username")).thenReturn("testuser");
        lenient().when(localizationService.resolveMessage(any()))
            .thenAnswer(invocation -> {
                Object error = invocation.getArgument(0);
                if (error instanceof org.springframework.validation.ObjectError objectError) {
                    return objectError.getDefaultMessage();
                }
                return "";
            });

            // When
            String result = authController.logout(session);

            // Then
            assertThat(result).isEqualTo("redirect:/login?logout=true");
            verify(session).invalidate();
        }

        @Test
        @DisplayName("Should invalidate session even when no username in session")
        void logout_WithoutUsername() {
            // Given
            when(session.getAttribute("username")).thenReturn(null);

            // When
            String result = authController.logout(session);

            // Then
            assertThat(result).isEqualTo("redirect:/login?logout=true");
            verify(session).invalidate();
        }
    }

    // ========== CHANGE PASSWORD METHOD TESTS ==========

    @Nested
    @DisplayName("ChangePassword Method Tests")
    class ChangePasswordTests {

        @Mock
        private RedirectAttributes redirectAttributes;

        @Test
        @DisplayName("Should successfully change password")
        void changePassword_Success() {
            // Given
            when(session.getAttribute("access_token")).thenReturn("valid-token");
            when(session.getAttribute("username")).thenReturn("testuser");
        lenient().when(localizationService.resolveMessage(any()))
            .thenAnswer(invocation -> {
                Object error = invocation.getArgument(0);
                if (error instanceof org.springframework.validation.ObjectError objectError) {
                    return objectError.getDefaultMessage();
                }
                return "";
            });
            doNothing().when(authService).changePassword(any(ChangePasswordRequest.class), anyString());

            // When
            String result = authController.changePassword(session, "newPassword123", redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(authService).changePassword(any(ChangePasswordRequest.class), eq("valid-token"));
            verify(redirectAttributes).addFlashAttribute("success", "Password changed successfully");
            verify(redirectAttributes, never()).addFlashAttribute(eq("error"), anyString());
        }

        @Test
        @DisplayName("Should return error when token is missing")
        void changePassword_MissingToken() {
            // Given
            when(session.getAttribute("access_token")).thenReturn(null);
            when(session.getAttribute("username")).thenReturn("testuser");
        lenient().when(localizationService.resolveMessage(any()))
            .thenAnswer(invocation -> {
                Object error = invocation.getArgument(0);
                if (error instanceof org.springframework.validation.ObjectError objectError) {
                    return objectError.getDefaultMessage();
                }
                return "";
            });

            // When
            String result = authController.changePassword(session, "newPassword123", redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/login");
            verify(redirectAttributes).addFlashAttribute("error", "Your session has expired. Please log in again.");
            verify(authService, never()).changePassword(any(), anyString());
        }

        @Test
        @DisplayName("Should return error when username is missing")
        void changePassword_MissingUsername() {
            // Given
            when(session.getAttribute("access_token")).thenReturn("valid-token");
            when(session.getAttribute("username")).thenReturn(null);

            // When
            String result = authController.changePassword(session, "newPassword123", redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/login");
            verify(redirectAttributes).addFlashAttribute("error", "Your session has expired. Please log in again.");
            verify(authService, never()).changePassword(any(), anyString());
        }

        @Test
        @DisplayName("Should handle BusinessException and return user-friendly error")
        void changePassword_BusinessException() {
            // Given
            when(session.getAttribute("access_token")).thenReturn("valid-token");
            when(session.getAttribute("username")).thenReturn("testuser");
        lenient().when(localizationService.resolveMessage(any()))
            .thenAnswer(invocation -> {
                Object error = invocation.getArgument(0);
                if (error instanceof org.springframework.validation.ObjectError objectError) {
                    return objectError.getDefaultMessage();
                }
                return "";
            });
            doThrow(new BusinessException("Failed to change password"))
                .when(authService).changePassword(any(ChangePasswordRequest.class), anyString());

            // When
            String result = authController.changePassword(session, "newPassword123", redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Failed to change password. Please try again.");
            verify(redirectAttributes, never()).addFlashAttribute(eq("success"), anyString());
        }

        @Test
        @DisplayName("Should handle generic exception without exposing details")
        void changePassword_GenericException() {
            // Given
            when(session.getAttribute("access_token")).thenReturn("valid-token");
            when(session.getAttribute("username")).thenReturn("testuser");
        lenient().when(localizationService.resolveMessage(any()))
            .thenAnswer(invocation -> {
                Object error = invocation.getArgument(0);
                if (error instanceof org.springframework.validation.ObjectError objectError) {
                    return objectError.getDefaultMessage();
                }
                return "";
            });
            doThrow(new RuntimeException("Database connection timeout at line 456"))
                .when(authService).changePassword(any(ChangePasswordRequest.class), anyString());

            // When
            String result = authController.changePassword(session, "newPassword123", redirectAttributes);

            // Then
            assertThat(result).isEqualTo("redirect:/home");
            verify(redirectAttributes).addFlashAttribute("error", "Failed to change password. Please try again.");
            verify(redirectAttributes, never()).addFlashAttribute(eq("success"), anyString());
        }
    }
}
