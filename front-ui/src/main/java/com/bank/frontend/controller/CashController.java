package com.bank.frontend.controller;

import com.bank.common.dto.contracts.cash.CashOperationRequest;
import com.bank.common.util.ErrorMessageUtil;
import com.bank.frontend.service.CashServiceClient;
import com.bank.frontend.service.LocalizationService;
import com.bank.frontend.util.MessageHelper;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.SmartValidator;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.stream.Collectors;


/**
 * Controller for managing cash operations (deposits and withdrawals).
 */
@Controller
@RequestMapping("/cash")
@RequiredArgsConstructor
@Slf4j
public class CashController {

    private final CashServiceClient cashServiceClient;
    private final LocalizationService localizationService;
    private final SmartValidator validator;

    /**
     * Processes a cash deposit to a bank account.
     *
     * @param request            the cash operation request
     * @param bindingResult      validation result
     * @param session            HTTP session
     * @param redirectAttributes attributes for redirect
     * @return redirect to home page
     */
    @PostMapping("/deposit")
    public String deposit(@ModelAttribute CashOperationRequest request,
                         BindingResult bindingResult,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {

        String username = (String) session.getAttribute("username");
        log.debug("Processing deposit for user: {}, accountId: {}, amount: {}",
            username, request.getBankAccountId(), request.getAmount());

        // Set operation type
        request.setType("DEPOSIT");
        validator.validate(request, bindingResult);

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                .map(localizationService::resolveMessage)
                .collect(Collectors.joining(", "));
            log.warn("Validation failed for deposit: {}", errorMessage);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/home";
        }

        try {
            cashServiceClient.deposit(request);
            log.info("Deposit completed successfully for user: {}, accountId: {}, amount: {}",
                username, request.getBankAccountId(), request.getAmount());
            redirectAttributes.addFlashAttribute("success", localizationService.getMessage("cash.depositSuccess"));
        } catch (Exception e) {
            log.error("Deposit failed for user {}, accountId {}: {}", username, request.getBankAccountId(),
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
                    errorMessage = localizationService.getMessage("cash.depositError");
                }
            }

            redirectAttributes.addFlashAttribute("error", errorMessage);
        }

        return "redirect:/home";
    }

    /**
     * Processes a cash withdrawal from a bank account.
     *
     * @param request            the cash operation request
     * @param bindingResult      validation result
     * @param session            HTTP session
     * @param redirectAttributes attributes for redirect
     * @return redirect to home page
     */
    @PostMapping("/withdraw")
    public String withdraw(@ModelAttribute CashOperationRequest request,
                          BindingResult bindingResult,
                          HttpSession session,
                          RedirectAttributes redirectAttributes) {

        String username = (String) session.getAttribute("username");
        log.debug("Processing withdrawal for user: {}, accountId: {}, amount: {}",
            username, request.getBankAccountId(), request.getAmount());

        // Set operation type
        request.setType("WITHDRAWAL");
        validator.validate(request, bindingResult);

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                .map(localizationService::resolveMessage)
                .collect(Collectors.joining(", "));
            log.warn("Validation failed for withdrawal: {}", errorMessage);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/home";
        }

        try {
            cashServiceClient.withdraw(request);
            log.info("Withdrawal completed successfully for user: {}, accountId: {}, amount: {}",
                username, request.getBankAccountId(), request.getAmount());
            redirectAttributes.addFlashAttribute("success", localizationService.getMessage("cash.withdrawSuccess"));
        } catch (Exception e) {
            log.error("Withdrawal failed for user {}, accountId {}: {}", username, request.getBankAccountId(),
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
                    errorMessage = localizationService.getMessage("cash.withdrawError");
                }
            }

            redirectAttributes.addFlashAttribute("error", errorMessage);
        }

        return "redirect:/home";
    }
}
