package com.bank.frontend.controller;

import com.bank.common.dto.contracts.accounts.CreateBankAccountRequest;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;


/**
 * Controller for managing bank account operations.
 */
@Controller
@RequestMapping("/bank-account")
@RequiredArgsConstructor
@Slf4j
public class BankAccountController {

    private final AccountServiceClient accountServiceClient;
    private final LocalizationService localizationService;

    /**
     * Creates a new bank account in the specified currency.
     *
     * @param request            the create bank account request
     * @param bindingResult      validation result
     * @param session            HTTP session
     * @param redirectAttributes attributes for redirect
     * @return redirect to home page
     */
    @PostMapping("/create")
    public String createBankAccount(@Valid @ModelAttribute CreateBankAccountRequest request,
                                    BindingResult bindingResult,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {

        String username = (String) session.getAttribute("username");
        log.debug("Processing bank account creation for user: {} with currency: {}", username, request.getCurrency());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                .map(localizationService::resolveMessage)
                .collect(Collectors.joining(", "));
            log.warn("Validation failed for bank account creation: {}", errorMessage);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/home";
        }

        try {
            accountServiceClient.createBankAccount(request);
            log.info("Bank account created successfully for user: {} with currency: {}", username, request.getCurrency());
            redirectAttributes.addFlashAttribute("success", localizationService.getMessage("bankAccount.create.success"));
        } catch (Exception e) {
            log.error("Failed to create bank account for user {}: {}", username,
                ErrorMessageUtil.sanitizeForLogging(e.getMessage()));

            // Extract the actual error message from JSON response
            String backendMessage = MessageHelper.extractReadable(e.getMessage());
            String localizationKey = MessageHelper.getLocalizationKey(backendMessage);

            String errorMessage = null;
            if (localizationKey != null) {
                // Try to use localized message if mapping exists
                try {
                    errorMessage = localizationService.getMessage(localizationKey);
                } catch (Exception ex) {
                    log.debug("Failed to get localized message for key: {}", localizationKey);
                }
            }

            // Fall back to backend message if localization didn't work
            if (errorMessage == null || errorMessage.isBlank()) {
                if (backendMessage != null && !backendMessage.isBlank()) {
                    errorMessage = backendMessage;
                } else {
                    // Final fallback to default error message
                    errorMessage = localizationService.getMessage("bankAccount.create.error");
                }
            }

            redirectAttributes.addFlashAttribute("error", errorMessage);
        }

        return "redirect:/home";
    }

    /**
     * Deletes an existing bank account.
     *
     * @param bankAccountId      the ID of the bank account to delete
     * @param session            HTTP session
     * @param redirectAttributes attributes for redirect
     * @return redirect to home page
     */
    @PostMapping("/delete")
    public String deleteBankAccount(@RequestParam Long bankAccountId,
                                    HttpSession session,
                                    RedirectAttributes redirectAttributes) {

        String username = (String) session.getAttribute("username");
        log.debug("Processing bank account deletion for user: {}, accountId: {}", username, bankAccountId);

        // Validate bank account ID
        if (bankAccountId == null || bankAccountId <= 0) {
            log.warn("Invalid bank account ID provided: {}", bankAccountId);
            redirectAttributes.addFlashAttribute("error", localizationService.getMessage("bankAccount.invalidId"));
            return "redirect:/home";
        }

        try {
            accountServiceClient.deleteBankAccount(bankAccountId);
            log.info("Bank account deleted successfully for user: {}, accountId: {}", username, bankAccountId);
            redirectAttributes.addFlashAttribute("success", localizationService.getMessage("bankAccount.delete.success"));
        } catch (Exception e) {
            log.error("Failed to delete bank account for user {}, accountId {}: {}", username, bankAccountId,
                ErrorMessageUtil.sanitizeForLogging(e.getMessage()));

            // Extract the actual error message from JSON response
            String backendMessage = MessageHelper.extractReadable(e.getMessage());
            String localizationKey = MessageHelper.getLocalizationKey(backendMessage);

            String errorMessage = null;
            if (localizationKey != null) {
                // Try to use localized message if mapping exists
                try {
                    errorMessage = localizationService.getMessage(localizationKey);
                } catch (Exception ex) {
                    log.debug("Failed to get localized message for key: {}", localizationKey);
                }
            }

            // Fall back to backend message if localization didn't work
            if (errorMessage == null || errorMessage.isBlank()) {
                if (backendMessage != null && !backendMessage.isBlank()) {
                    errorMessage = backendMessage;
                } else {
                    // Final fallback to default error message
                    errorMessage = localizationService.getMessage("bankAccount.delete.error");
                }
            }

            redirectAttributes.addFlashAttribute("error", errorMessage);
        }

        return "redirect:/home";
    }
}
