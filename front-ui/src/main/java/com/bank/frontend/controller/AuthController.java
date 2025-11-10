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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private static final String LOGIN_BASE_PATH = "/login";
    private static final String REGISTER_BASE_PATH = "/register";
    public static final String ACCESS_TOKEN = "access_token";
    private final AuthService authService;
    private final AccountsClient accountsClient;
    private final LocalizationService localizationService;

    @GetMapping("/")
    public String index(HttpSession session) {
        String token = (String) session.getAttribute(ACCESS_TOKEN);

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

        String token = (String) session.getAttribute(ACCESS_TOKEN);
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
                session.setAttribute(ACCESS_TOKEN, tokens.accessToken());
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
        String token = (String) session.getAttribute(ACCESS_TOKEN);
        if (token != null) {
            return "redirect:/home";
        }

        log.debug("Accessing register page");
        model.addAttribute("registerRequest", RegisterRequest.NULL_REQUEST);
        return "register";
    }

    @PostMapping("/perform-register")
    public String performRegister(
        @Valid @ModelAttribute RegisterRequest registerRequest,
        BindingResult bindingResult,
        Model model) {

        log.debug("Processing registration for user: {}", registerRequest.username());
        model.addAttribute("languageLinks", buildLanguageLinks(REGISTER_BASE_PATH));

        if (!registerRequest.password().equals(registerRequest.confirmPassword())) {
            String mismatchMessage = localizationService.getMessage("message.passwordMismatch");
            bindingResult.rejectValue("confirmPassword", "message.passwordMismatch", null, mismatchMessage);
            model.addAttribute("error", mismatchMessage);
            model.addAttribute("registerRequest", registerRequest);
            return "register";
        }

        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                .map(this::resolveValidationMessage)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.joining(", "));
            if (!StringUtils.hasText(errorMessage)) {
                errorMessage = localizationService.getMessage("register.error.details");
            }
            model.addAttribute("error", errorMessage);
            model.addAttribute("registerRequest", registerRequest);
            return "register";
        }

        try {
            var response = authService.register(registerRequest);
            if (!response.isSuccess()) {
                if (log.isWarnEnabled()) {
                    log.warn("Registration failed for user {}: {}", registerRequest.username(),
                        ErrorMessageUtil.sanitizeForLogging(response.getMessage()));
                }

                String backendMessage = com.bank.frontend.util.MessageHelper.extractReadable(response.getMessage());
                log.debug("Extracted backend message: '{}'", backendMessage);

                String errorMessage = null;

                if (backendMessage != null && backendMessage.toLowerCase().contains("username already exists")) {
                    String baseMessage = localizationService.getMessage("register.error.usernameExists");

                    String suggestedUsername = generateAlternativeUsername(
                        registerRequest.username(),
                        registerRequest.firstName(),
                        registerRequest.lastName()
                    );

                    if (suggestedUsername != null && !suggestedUsername.equals(registerRequest.username() + java.time.Year.now().getValue())) {
                        errorMessage = baseMessage + " " +
                            localizationService.getMessage("register.error.trySuggestion", suggestedUsername);
                    } else {
                        errorMessage = baseMessage;
                    }
                } else if (backendMessage != null && !backendMessage.isBlank()) {
                    String messageKey = com.bank.frontend.util.MessageHelper.toMessageKey(backendMessage);
                    if (messageKey != null) {
                        errorMessage = localizationService.getMessageOrDefault(messageKey, backendMessage);
                    } else {
                        errorMessage = backendMessage;
                    }
                }

                if (errorMessage == null || errorMessage.isBlank()) {
                    errorMessage = localizationService.getMessage("register.error.details");
                }

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
                    model.addAttribute("error", localizationService.getMessage("register.account.create.failure"));
                    model.addAttribute("registerRequest", registerRequest);
                    return "register";
                }
                log.info("User {} registered successfully", registerRequest.username());
                return "redirect:/login?registered=true";
            }

        } catch (Exception e) {
            log.error("Registration error for user {}: {}", registerRequest.username(), e.getMessage());

            String validationErrors = com.bank.frontend.util.MessageHelper.extractValidationErrors(e.getMessage());
            if (validationErrors != null && !validationErrors.isBlank()) {
                log.debug("Extracted validation errors: {}", validationErrors);

                String messageKey = com.bank.frontend.util.MessageHelper.toMessageKey(validationErrors);
                if (messageKey != null) {
                    String translatedError = localizationService.getMessageOrDefault(messageKey, validationErrors);
                    model.addAttribute("error", translatedError);
                } else {
                    model.addAttribute("error", validationErrors);
                }
            } else {
                model.addAttribute("error", localizationService.getMessage("register.service.unavailable"));
            }
            model.addAttribute("registerRequest", registerRequest);
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

    private String resolveValidationMessage(ObjectError error) {
        Object[] arguments = error.getArguments();

        String directMessage = tryResolveMessage(localizationService.resolveMessage(error), arguments, false);
        if (StringUtils.hasText(directMessage)) {
            return directMessage;
        }

        String[] codes = error.getCodes();
        if (codes != null) {
            for (String code : codes) {
                String resolved = tryResolveMessage(code, arguments, false);
                if (StringUtils.hasText(resolved)) {
                    return resolved;
                }
            }
        }

        String defaultMessage = error.getDefaultMessage();
        if (StringUtils.hasText(defaultMessage)) {
            String resolvedDefault = tryResolveMessage(defaultMessage, arguments, true);
            if (StringUtils.hasText(resolvedDefault)) {
                return resolvedDefault;
            }
            return defaultMessage;
        }
        return "";
    }

    private String tryResolveMessage(String candidate, Object[] arguments, boolean allowFallback) {
        if (!StringUtils.hasText(candidate)) {
            return "";
        }

        String trimmed = candidate.trim();

        if (!looksLikeMessageKey(trimmed)) {
            return trimmed;
        }

        String key = extractMessageKey(trimmed);
        if (!StringUtils.hasText(key)) {
            return allowFallback ? trimmed : "";
        }

        try {
            String resolved = localizationService.getMessage(key, arguments);
            if (StringUtils.hasText(resolved)) {
                return resolved;
            }
        } catch (Exception ex) {
            if (log.isTraceEnabled()) {
                log.trace("Failed to resolve message for key '{}': {}", key, ex.getMessage());
            }
        }

        return allowFallback ? key : "";
    }

    private boolean looksLikeMessageKey(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.length() > 2) {
            return true;
        }
        return trimmed.matches("[a-zA-Z0-9_.-]+");
    }

    private String extractMessageKey(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.length() > 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
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
            if (!suggestion.isEmpty()) {
                suggestion.append(".");
            }
            suggestion.append(lastName.toLowerCase().trim());
        }

        // If we couldn't build from name, use the original username
        if (suggestion.isEmpty() && username != null && !username.isBlank()) {
            suggestion.append(username);
        }

        // Add year to make it unique
        if (!suggestion.isEmpty()) {
            suggestion.append(java.time.Year.now().getValue());
        }

        return !suggestion.isEmpty() ? suggestion.toString() : null;
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
            String token = (String) session.getAttribute(ACCESS_TOKEN);
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
