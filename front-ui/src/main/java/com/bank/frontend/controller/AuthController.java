package com.bank.frontend.controller;

import com.bank.common.dto.contracts.accounts.ChangePasswordRequest;
import com.bank.common.dto.contracts.accounts.CreateAccountRequest;
import com.bank.common.dto.contracts.auth.LoginRequest;
import com.bank.common.dto.contracts.auth.RegisterRequest;
import com.bank.common.dto.contracts.auth.TokenResponse;
import com.bank.common.util.ErrorMessageUtil;
import com.bank.frontend.client.AccountsClient;
import com.bank.frontend.service.AuthService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final AccountsClient accountsClient;

    @GetMapping("/")
    public String index(HttpSession session) {
        String token = (String) session.getAttribute("access_token");

        if (token != null) {
            log.debug("User has active session, redirecting to home");
            return "redirect:/home";
        }
        log.debug("No active session, redirecting to login");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginPage(
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "logout", required = false) String logout,
        @RequestParam(name = "sessionExpired", required = false) String sessionExpired,
        @RequestParam(name = "registered", required = false) String registered,
        HttpSession session,
        Model model) {

        String token = (String) session.getAttribute("access_token");
        if (token != null) {
            return "redirect:/home";
        }

        log.debug("Accessing login page");

        if (error != null) {
            model.addAttribute("error", "Invalid username or password");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully");
        }
        if (sessionExpired != null) {
            model.addAttribute("error", "Your session has expired. Please log in again.");
        }
        if (registered != null) {
            model.addAttribute("message", "Registration successful. Please log in.");
        }
        model.addAttribute("loginRequest", new LoginRequest());
        return "login";
    }

    @PostMapping("/perform-login")
    public String performLogin(
        @ModelAttribute LoginRequest loginRequest,
        HttpSession session,
        Model model) {

        log.debug("Processing login for user: {}", loginRequest.getUsername());

        try {
            var response = authService.login(loginRequest);

            if (!response.isSuccess()) {
                log.warn("Login failed for user {}: {}", loginRequest.getUsername(), response.getMessage());
                model.addAttribute("error", "Invalid username or password");
                model.addAttribute("loginRequest", loginRequest);
                return "login";
            }
            TokenResponse tokens = response.getData();

            if (tokens != null && tokens.accessToken() != null) {
                session.setAttribute("access_token", tokens.accessToken());
                session.setAttribute("refresh_token", tokens.refreshToken());
                session.setAttribute("token_scope", tokens.scope());
                session.setAttribute("token_type", tokens.tokenType());
                session.setAttribute("username", loginRequest.getUsername());
                session.setMaxInactiveInterval(30 * 60);

                log.info("User {} logged in successfully", loginRequest.getUsername());
                return "redirect:/home";
            }

            log.warn("Login failed for user: {}", loginRequest.getUsername());
            model.addAttribute("error", "Invalid username or password");
            model.addAttribute("loginRequest", loginRequest);
            return "login";

        } catch (Exception e) {
            log.error("Login error for user {}: {}", loginRequest.getUsername(), e.getMessage());
            model.addAttribute("error", "Authentication service unavailable. Please try again later.");
            model.addAttribute("loginRequest", loginRequest);
            return "login";
        }
    }

    @GetMapping("/register")
    public String registerPage(HttpSession session, Model model) {
        String token = (String) session.getAttribute("access_token");
        if (token != null) {
            return "redirect:/home";
        }

        log.debug("Accessing register page");
        model.addAttribute("registerRequest", RegisterRequest.NULL_REQUEST);
        return "register";
    }

    @PostMapping("/perform-register")
    public String performRegister(
        @ModelAttribute RegisterRequest registerRequest,
        HttpSession session,
        Model model) {

        log.debug("Processing registration for user: {}", registerRequest.username());

        // Validate passwords match
        if (!registerRequest.password().equals(registerRequest.confirmPassword())) {
            model.addAttribute("error", "Passwords do not match");
            model.addAttribute("registerRequest", registerRequest);
            return "register";
        }

        // Validate password length
        if (registerRequest.password().length() < 6) {
            model.addAttribute("error", "Password must be at least 6 characters long");
            model.addAttribute("registerRequest", registerRequest);
            return "register";
        }

        try {
            var response = authService.register(registerRequest);
            if (!response.isSuccess()) {
                log.warn("Registration failed for user {}: {}", registerRequest.username(),
                    ErrorMessageUtil.sanitizeForLogging(response.getMessage()));
                // Return user-friendly error message
                String errorMessage = ErrorMessageUtil.extractUserFriendlyMessage(response.getMessage(),
                    "Registration failed. Please check your details and try again.");
                model.addAttribute("error", errorMessage);
                model.addAttribute("registerRequest", registerRequest);
                return "register";
            } else {
                CreateAccountRequest accountsRequest = CreateAccountRequest.builder()
                    .username(registerRequest.username())
                    .firstName(registerRequest.firstName())
                    .lastName(registerRequest.lastName())
                    .email(registerRequest.email())
                    .birthDate(registerRequest.birthDate())
                    .build();
                var createAccountResponse = accountsClient.createUserAccount(accountsRequest);
                if (!createAccountResponse.isSuccess()) {
                    log.error("Account creation failed for user {}: {}", registerRequest.username(), createAccountResponse.getMessage());
                    model.addAttribute("error", "Unable to create account. Please try again later.");
                    model.addAttribute("registerRequest", registerRequest);
                    return "register";
                }
                log.info("User {} registered successfully", registerRequest.username());
                return "redirect:/login?registered=true";
            }

        } catch (Exception e) {
            log.error("Registration error for user {}: {}", registerRequest.username(), e.getMessage());
            model.addAttribute("error", "Registration service unavailable. Please try again later.");
            model.addAttribute("registerRequest", registerRequest);
            return "register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        String username = (String) session.getAttribute("username");
        if (username != null) {
            log.info("User {} logged out", username);
        }
        session.invalidate();
        return "redirect:/login?logout=true";
    }

    @PostMapping("/change-password")
    public String changePassword(HttpSession session,
        @RequestParam String newPassword,
        RedirectAttributes redirectAttributes) {
        try {
            String token = (String) session.getAttribute("access_token");
            String username = (String) session.getAttribute("username");

            if (token == null || username == null) {
                redirectAttributes.addFlashAttribute("error", "Session expired. Please login again.");
                return "redirect:/login";
            }

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .username(username)
                .newPassword(newPassword)
                .build();

            authService.changePassword(request, token);
            redirectAttributes.addFlashAttribute("success", "Password changed successfully");
        } catch (Exception e) {
            log.error("Password change error for user {}: {}", session.getAttribute("username"), e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to change password. Please try again.");
        }

        return "redirect:/home";
    }

}
