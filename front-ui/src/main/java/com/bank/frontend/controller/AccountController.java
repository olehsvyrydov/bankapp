package com.bank.frontend.controller;

import com.bank.common.dto.contracts.accounts.UpdateAccountRequest;
import com.bank.common.util.ErrorMessageUtil;
import com.bank.frontend.service.AccountServiceClient;
import com.bank.frontend.service.LocalizationService;
import com.bank.frontend.util.MessageHelper;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;


/**
 * Controller for managing user account operations.
 */
@Controller
@RequestMapping("/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountServiceClient accountServiceClient;
    private final LocalizationService localizationService;

    /**
     * Updates user account information.
     * Validates input and displays appropriate success or error messages.
     *
     * @param request            the update account request with validated fields
     * @param bindingResult      validation result
     * @param session            HTTP session
     * @param redirectAttributes attributes for redirect
     * @return redirect to home page
     */
    @PostMapping("/update")
    public String updateAccount(@Valid @ModelAttribute UpdateAccountRequest request,
                                BindingResult bindingResult,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {

        String username = (String) session.getAttribute("username");
        log.debug("Processing account update for user: {}", username);

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                .map(localizationService::resolveMessage)
                .collect(Collectors.joining(", "));
            log.warn("Validation failed for account update: {}", errorMessage);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/home";
        }

        try {
            accountServiceClient.updateAccount(request);
            log.info("Account updated successfully for user: {}", username);
            redirectAttributes.addFlashAttribute("success", localizationService.getMessage("account.update.success"));
        } catch (Exception e) {
            log.error("Failed to update account for user {}: {}", username,
                ErrorMessageUtil.sanitizeForLogging(e.getMessage()));
            String defaultMessage = localizationService.getMessage("account.update.error");
            String friendlyMessage = ErrorMessageUtil.extractUserFriendlyMessage(
                e.getMessage(),
                defaultMessage
            );
            String rawMessage = ErrorMessageUtil.sanitizeForLogging(e.getMessage());
            String finalMessage = MessageHelper.pickUserMessage(defaultMessage, friendlyMessage, rawMessage);
            redirectAttributes.addFlashAttribute("error", finalMessage);
        }

        return "redirect:/home";
    }
}
