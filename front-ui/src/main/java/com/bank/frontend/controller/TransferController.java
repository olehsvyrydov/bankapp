package com.bank.frontend.controller;

import com.bank.common.dto.contracts.transfer.TransferRequest;
import com.bank.common.util.ErrorMessageUtil;
import com.bank.frontend.service.TransferServiceClient;
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
 * Controller for managing transfer operations between bank accounts.
 */
@Controller
@RequestMapping("/transfer")
@RequiredArgsConstructor
@Slf4j
public class TransferController {

    private final TransferServiceClient transferServiceClient;

    /**
     * Processes a transfer between user's own bank accounts.
     *
     * @param request            the transfer request
     * @param bindingResult      validation result
     * @param session            HTTP session
     * @param redirectAttributes attributes for redirect
     * @return redirect to home page
     */
    @PostMapping("/own")
    public String transferOwn(@Valid @ModelAttribute TransferRequest request,
                             BindingResult bindingResult,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {

        String username = (String) session.getAttribute("username");
        log.debug("Processing own transfer for user: {}, from: {}, to: {}, amount: {}",
            username, request.getFromBankAccountId(), request.getToBankAccountId(), request.getAmount());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));
            log.warn("Validation failed for transfer: {}", errorMessage);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/home";
        }

        // Business rule: cannot transfer to the same account
        if (request.getFromBankAccountId().equals(request.getToBankAccountId())) {
            log.warn("User {} attempted to transfer to the same account: {}", username, request.getFromBankAccountId());
            redirectAttributes.addFlashAttribute("error", "Cannot transfer to the same account");
            return "redirect:/home";
        }

        try {
            transferServiceClient.transfer(request);
            log.info("Transfer completed successfully for user: {}, from: {}, to: {}, amount: {}",
                username, request.getFromBankAccountId(), request.getToBankAccountId(), request.getAmount());
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully");
        } catch (Exception e) {
            log.error("Transfer failed for user {}: {}", username,
                ErrorMessageUtil.sanitizeForLogging(e.getMessage()));
            String friendlyMessage = ErrorMessageUtil.extractUserFriendlyMessage(
                e.getMessage(),
                "Transfer failed. Please try again."
            );
            redirectAttributes.addFlashAttribute("error", friendlyMessage);
        }

        return "redirect:/home";
    }

    /**
     * Processes a transfer to another user's bank account.
     *
     * @param request            the transfer request
     * @param bindingResult      validation result
     * @param session            HTTP session
     * @param redirectAttributes attributes for redirect
     * @return redirect to home page
     */
    @PostMapping("/other")
    public String transferOther(@Valid @ModelAttribute TransferRequest request,
                               BindingResult bindingResult,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {

        String username = (String) session.getAttribute("username");
        log.debug("Processing transfer to other for user: {}, from: {}, to: {}, amount: {}",
            username, request.getFromBankAccountId(), request.getToBankAccountId(), request.getAmount());

        // Check for validation errors
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .collect(Collectors.joining(", "));
            log.warn("Validation failed for transfer: {}", errorMessage);
            redirectAttributes.addFlashAttribute("error", errorMessage);
            return "redirect:/home";
        }

        // Business rule: cannot transfer to the same account
        if (request.getFromBankAccountId().equals(request.getToBankAccountId())) {
            log.warn("User {} attempted to transfer to the same account: {}", username, request.getFromBankAccountId());
            redirectAttributes.addFlashAttribute("error", "Cannot transfer to the same account");
            return "redirect:/home";
        }

        try {
            transferServiceClient.transfer(request);
            log.info("Transfer to other completed successfully for user: {}, from: {}, to: {}, amount: {}",
                username, request.getFromBankAccountId(), request.getToBankAccountId(), request.getAmount());
            redirectAttributes.addFlashAttribute("success", "Transfer completed successfully");
        } catch (Exception e) {
            log.error("Transfer to other failed for user {}: {}", username,
                ErrorMessageUtil.sanitizeForLogging(e.getMessage()));
            String friendlyMessage = ErrorMessageUtil.extractUserFriendlyMessage(
                e.getMessage(),
                "Transfer failed. Please try again."
            );
            redirectAttributes.addFlashAttribute("error", friendlyMessage);
        }

        return "redirect:/home";
    }
}
