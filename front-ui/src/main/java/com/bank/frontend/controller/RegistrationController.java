package com.bank.frontend.controller;

import com.bank.common.dto.contracts.accounts.CreateAccountRequest;
import com.bank.common.util.ErrorMessageUtil;
import com.bank.frontend.service.AccountServiceClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.stream.Collectors;

/**
 * Controller for user registration operations.
 * Note: This endpoint is public and doesn't require authentication.
 */
@Controller
@Slf4j
@RequiredArgsConstructor
public class RegistrationController {

    private final AccountServiceClient accountServiceClient;

    /**
     * Processes user registration.
     *
     * @param request       the registration request with validated data
     * @param bindingResult validation result
     * @param model         model for view attributes
     * @return redirect to login on success or register page on failure
     */
    @PostMapping("/register")
    public String register(@Valid @ModelAttribute CreateAccountRequest request,
                          BindingResult bindingResult,
                          Model model) {

        log.debug("Registration attempt for username: {}", request.getUsername());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));
            log.warn("Validation failed for registration of user {}: {}", request.getUsername(), errorMessage);
            model.addAttribute("error", errorMessage);
            model.addAttribute("createAccountRequest", request);
            return "register";
        }

        try {
            log.info("Creating account for user: {}", request.getUsername());
            accountServiceClient.createAccount(request);
            log.info("Account created successfully for user: {}", request.getUsername());
            return "redirect:/login?registered";
        } catch (Exception e) {
            log.error("Registration failed for user {}: {}", request.getUsername(),
                ErrorMessageUtil.sanitizeForLogging(e.getMessage()));
            String friendlyMessage = ErrorMessageUtil.extractUserFriendlyMessage(
                e.getMessage(),
                "Registration failed. Please try again."
            );
            model.addAttribute("error", friendlyMessage);
            model.addAttribute("createAccountRequest", request);
            return "register";
        }
    }
}
