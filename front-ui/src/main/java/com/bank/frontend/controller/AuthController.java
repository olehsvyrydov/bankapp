package com.bank.frontend.controller;

import com.bank.common.dto.contracts.accounts.ChangePasswordRequest;
import com.bank.common.dto.contracts.accounts.CreateAccountRequest;
import com.bank.common.dto.contracts.auth.LoginRequest;
import com.bank.common.dto.contracts.auth.RegisterRequest;
import com.bank.common.dto.contracts.auth.TokenResponse;
import com.bank.common.util.ErrorMessageUtil;
import com.bank.frontend.client.AccountsClient;
import com.bank.frontend.service.AuthService;
import com.bank.frontend.service.LocalizationService;
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
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.Period;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String LOGIN_BASE_PATH = "/login";
    private static final String REGISTER_BASE_PATH = "/register";
    private static final int MIN_REGISTRATION_AGE = 18;

    private final AuthService authService;
    private final AccountsClient accountsClient;
    private final LocalizationService localizationService;

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
            model.addAttribute("error", localizationService.getMessage("auth.invalidCredentials"));
        }
        if (logout != null) {
            model.addAttribute("message", localizationService.getMessage("auth.logout.success"));
        }
        if (sessionExpired != null) {
            model.addAttribute("error", localizationService.getMessage("auth.session.expired"));
        }
        if (registered != null) {
            model.addAttribute("message", localizationService.getMessage("register.success.login"));
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
                model.addAttribute("error", localizationService.getMessage("auth.invalidCredentials"));
                model.addAttribute("loginRequest", loginRequest);
                model.addAttribute("languageLinks", buildLanguageLinks(LOGIN_BASE_PATH));
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
            model.addAttribute("error", localizationService.getMessage("auth.invalidCredentials"));
            model.addAttribute("loginRequest", loginRequest);
            model.addAttribute("languageLinks", buildLanguageLinks(LOGIN_BASE_PATH));
            return "login";

        } catch (Exception e) {
            log.error("Login error for user {}: {}", loginRequest.getUsername(), e.getMessage());
            model.addAttribute("error", localizationService.getMessage("auth.service.unavailable"));
            model.addAttribute("loginRequest", loginRequest);
            model.addAttribute("languageLinks", buildLanguageLinks(LOGIN_BASE_PATH));
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
            model.addAttribute("error", localizationService.getMessage("message.passwordMismatch"));
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("languageLinks", buildLanguageLinks(REGISTER_BASE_PATH));
            return "register";
        }

        // Validate password length
        if (registerRequest.password().length() < 6) {
            model.addAttribute("error", localizationService.getMessage("message.passwordTooShort"));
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("languageLinks", buildLanguageLinks(REGISTER_BASE_PATH));
            return "register";
        }

        LocalDate birthDate = registerRequest.birthDate();
        if (birthDate == null) {
            model.addAttribute("error", localizationService.getMessage("validation.birthDate.required"));
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("languageLinks", buildLanguageLinks(REGISTER_BASE_PATH));
            return "register";
        }

        if (!birthDate.isBefore(LocalDate.now())) {
            model.addAttribute("error", localizationService.getMessage("validation.birthDate.past"));
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("languageLinks", buildLanguageLinks(REGISTER_BASE_PATH));
            return "register";
        }

        if (!hasMinimumAge(birthDate)) {
            model.addAttribute("error", localizationService.getMessage("message.ageTooYoung"));
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("languageLinks", buildLanguageLinks(REGISTER_BASE_PATH));
            return "register";
        }

        try {
            var response = authService.register(registerRequest);
            if (!response.isSuccess()) {
                log.warn("Registration failed for user {}: {}", registerRequest.username(),
                    ErrorMessageUtil.sanitizeForLogging(response.getMessage()));

                // Extract the actual error message from response
                String backendMessage = com.bank.frontend.util.MessageHelper.extractReadable(response.getMessage());
                log.debug("Extracted backend message: '{}'", backendMessage);

                String errorMessage = null;

                // Check if username already exists
                if (backendMessage != null && backendMessage.toLowerCase().contains("username already exists")) {
                    String baseMessage = localizationService.getMessage("register.error.usernameExists");

                    // Try to generate alternative username suggestion
                    String suggestedUsername = generateAlternativeUsername(
                        registerRequest.username(),
                        registerRequest.firstName(),
                        registerRequest.lastName()
                    );

                    // Only add suggestion if it's valid (not just the original username with year)
                    if (suggestedUsername != null && !suggestedUsername.equals(registerRequest.username() + java.time.Year.now().getValue())) {
                        errorMessage = baseMessage + " " +
                            localizationService.getMessage("register.error.trySuggestion", suggestedUsername);
                    } else {
                        errorMessage = baseMessage;
                    }
                } else if (backendMessage != null && !backendMessage.isBlank()) {
                    // Try to translate the backend message
                    String messageKey = com.bank.frontend.util.MessageHelper.toMessageKey(backendMessage);
                    if (messageKey != null) {
                        errorMessage = localizationService.getMessageOrDefault(messageKey, backendMessage);
                    } else {
                        errorMessage = backendMessage;
                    }
                }

                // Final fallback to generic message if nothing worked
                if (errorMessage == null || errorMessage.isBlank()) {
                    errorMessage = localizationService.getMessage("register.error.details");
                }

                model.addAttribute("error", errorMessage);
                model.addAttribute("registerRequest", registerRequest);
                model.addAttribute("languageLinks", buildLanguageLinks(REGISTER_BASE_PATH));
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
                    model.addAttribute("error", localizationService.getMessage("register.account.create.failure"));
                    model.addAttribute("registerRequest", registerRequest);
                    model.addAttribute("languageLinks", buildLanguageLinks(REGISTER_BASE_PATH));
                    return "register";
                }
                log.info("User {} registered successfully", registerRequest.username());
                return "redirect:/login?registered=true";
            }

        } catch (Exception e) {
            log.error("Registration error for user {}: {}", registerRequest.username(), e.getMessage());

            // Try to extract validation errors from the exception message
            String validationErrors = com.bank.frontend.util.MessageHelper.extractValidationErrors(e.getMessage());
            if (validationErrors != null && !validationErrors.isBlank()) {
                log.debug("Extracted validation errors: {}", validationErrors);

                // Try to translate the validation error if it matches a known pattern
                String messageKey = com.bank.frontend.util.MessageHelper.toMessageKey(validationErrors);
                if (messageKey != null) {
                    String translatedError = localizationService.getMessageOrDefault(messageKey, validationErrors);
                    model.addAttribute("error", translatedError);
                } else {
                    model.addAttribute("error", validationErrors);
                }
            } else {
                // Fall back to generic service unavailable message
                model.addAttribute("error", localizationService.getMessage("register.service.unavailable"));
            }
            model.addAttribute("registerRequest", registerRequest);
            model.addAttribute("languageLinks", buildLanguageLinks(REGISTER_BASE_PATH));
            return "register";
        }
    }

    private Map<String, String> buildLanguageLinks(String basePath) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(basePath);
        Map<String, String> links = new HashMap<>();
        links.put("en", builder.cloneBuilder().replaceQueryParam("lang", "en").build().toUriString());
        links.put("ru", builder.cloneBuilder().replaceQueryParam("lang", "ru").build().toUriString());
        return links;
    }

    private boolean hasMinimumAge(LocalDate birthDate) {
        return Period.between(birthDate, LocalDate.now()).getYears() >= MIN_REGISTRATION_AGE;
    }

    /**
     * Generates alternative username suggestions based on user's data.
     *
     * @param username  the original username
     * @param firstName user's first name
     * @param lastName  user's last name
     * @return suggested alternative username, or username with year if no name data available
     */
    private String generateAlternativeUsername(String username, String firstName, String lastName) {
        // Generate suggestions based on name and random numbers
        StringBuilder suggestion = new StringBuilder();

        // Only use first name if it's not null and not empty
        if (firstName != null && !firstName.isBlank()) {
            suggestion.append(firstName.toLowerCase().trim());
        }

        // Only use last name if it's not null and not empty
        if (lastName != null && !lastName.isBlank()) {
            if (suggestion.length() > 0) {
                suggestion.append(".");
            }
            suggestion.append(lastName.toLowerCase().trim());
        }

        // If we couldn't build from name, use the original username
        if (suggestion.length() == 0 && username != null && !username.isBlank()) {
            suggestion.append(username);
        }

        // Add year to make it unique
        if (suggestion.length() > 0) {
            suggestion.append(java.time.Year.now().getValue());
        }

        return suggestion.length() > 0 ? suggestion.toString() : null;
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
                redirectAttributes.addFlashAttribute("error", localizationService.getMessage("auth.session.expired"));
                return "redirect:/login";
            }

            ChangePasswordRequest request = ChangePasswordRequest.builder()
                .username(username)
                .newPassword(newPassword)
                .build();

            authService.changePassword(request, token);
            redirectAttributes.addFlashAttribute("success", localizationService.getMessage("auth.password.change.success"));
        } catch (Exception e) {
            log.error("Password change error for user {}: {}", session.getAttribute("username"), e.getMessage());
            redirectAttributes.addFlashAttribute("error", localizationService.getMessage("auth.password.change.error"));
        }

        return "redirect:/home";
    }

}
