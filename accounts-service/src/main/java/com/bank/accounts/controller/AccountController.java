package com.bank.accounts.controller;

import com.bank.accounts.service.AccountService;
import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AccountDTO>> register(@Valid @RequestBody CreateAccountRequest request) {
        AccountDTO account = accountService.createAccount(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(account, "Account created successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AccountDTO>> getCurrentAccount() {
        String username = resolveUsername();
        AccountDTO account = accountService.getAccountByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<AccountDTO>> updateMyAccount(
        @Valid @RequestBody UpdateAccountRequest request) {
        String username = resolveUsername();
        AccountDTO account = accountService.updateAccount(username, request);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/me/bank-accounts")
    public ResponseEntity<ApiResponse<BankAccountDTO>> createBankAccount(
        @Valid @RequestBody CreateBankAccountRequest request) {
        String username = resolveUsername();
        BankAccountDTO bankAccount = accountService.createBankAccount(username, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(bankAccount));
    }

    @GetMapping("/me/bank-accounts")
    public ResponseEntity<ApiResponse<List<BankAccountDTO>>> getMyBankAccounts() {
        String username = resolveUsername();
        List<BankAccountDTO> bankAccounts = accountService.getBankAccountsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(bankAccounts));
    }

    @GetMapping("/me/bank-accounts/{id}")
    public ResponseEntity<ApiResponse<BankAccountDTO>> getMyBankAccount(
        @PathVariable("id") Long id) {
        String username = resolveUsername();
        BankAccountDTO bankAccount = accountService.getBankAccountById(id, username);
        return ResponseEntity.ok(ApiResponse.success(bankAccount));
    }

    @DeleteMapping("/me/bank-accounts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMyBankAccount(
        @PathVariable("id") Long id) {
        String username = resolveUsername();
        accountService.deleteBankAccount(id, username);
        return ResponseEntity.ok(ApiResponse.success(null, "Bank account deleted successfully"));
    }

    @PostMapping("/bank-accounts/update-balance")
    public ResponseEntity<ApiResponse<BankAccountDTO>> updateBalance(
        @RequestBody UpdateBalanceRequest request) {
        BankAccountDTO bankAccount = accountService.updateBalance(request);
        return ResponseEntity.ok(ApiResponse.success(bankAccount));
    }

    @GetMapping("/bank-accounts/{id}")
    public ResponseEntity<ApiResponse<BankAccountDTO>> getBankAccountByIdPublic(@PathVariable("id") Long id) {
        BankAccountDTO bankAccount = accountService.getBankAccountByIdPublic(id);
        return ResponseEntity.ok(ApiResponse.success(bankAccount));
    }

    @GetMapping("/by-email/{email}")
    public ResponseEntity<ApiResponse<List<BankAccountDTO>>> getBankAccountsByEmail(@PathVariable("email") String email) {
        log.debug("Finding bank accounts for user with email: {}", email);
        try {
            AccountDTO account = accountService.getAccountByEmail(email);
            if (account == null || account.getBankAccounts() == null) {
                log.warn("No user or bank accounts found for email: {}", email);
                return ResponseEntity.ok(ApiResponse.error("No user found with email: " + email));
            }
            log.info("Found {} bank accounts for email: {}", account.getBankAccounts().size(), email);
            return ResponseEntity.ok(ApiResponse.success(account.getBankAccounts()));
        } catch (Exception e) {
            log.error("Error finding accounts by email {}: {}", email, e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("No user found with email: " + email));
        }
    }

    private String resolveUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof AnonymousAuthenticationToken token) {
            log.warn("resolveUsername: authentication missing or anonymous. Authentication={}", token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        log.debug("resolveUsername: authenticated user={}, type={}",
            auth.getName(), auth.getClass().getSimpleName());
        return auth.getName();
    }
}
