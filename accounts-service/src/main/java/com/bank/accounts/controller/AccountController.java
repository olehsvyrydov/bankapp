package com.bank.accounts.controller;

import com.bank.accounts.service.AccountService;
import com.bank.common.annotations.CurrentUsername;
import com.bank.common.dto.ApiResponse;
import com.bank.common.dto.contracts.accounts.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ResponseEntity<ApiResponse<AccountDTO>> getCurrentAccount(
        @CurrentUsername String username) {
        AccountDTO account = accountService.getAccountByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<AccountDTO>> updateMyAccount(
        @Valid @RequestBody UpdateAccountRequest request,
        @CurrentUsername String username) {
        AccountDTO account = accountService.updateAccount(username, request);
        return ResponseEntity.ok(ApiResponse.success(account));
    }

    @PostMapping("/me/bank-accounts")
    public ResponseEntity<ApiResponse<BankAccountDTO>> createBankAccount(
        @Valid @RequestBody CreateBankAccountRequest request,
        @CurrentUsername String username) {
        BankAccountDTO bankAccount = accountService.createBankAccount(username, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(bankAccount));
    }

    @GetMapping("/me/bank-accounts")
    public ResponseEntity<ApiResponse<List<BankAccountDTO>>> getMyBankAccounts(
        @CurrentUsername String username) {
        List<BankAccountDTO> bankAccounts = accountService.getBankAccountsByUsername(username);
        return ResponseEntity.ok(ApiResponse.success(bankAccounts));
    }

    @GetMapping("/me/bank-accounts/{id}")
    public ResponseEntity<ApiResponse<BankAccountDTO>> getMyBankAccount(
        @PathVariable("id") Long id,
        @CurrentUsername String username) {
        BankAccountDTO bankAccount = accountService.getBankAccountById(id, username);
        return ResponseEntity.ok(ApiResponse.success(bankAccount));
    }

    @DeleteMapping("/me/bank-accounts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMyBankAccount(
        @PathVariable("id") Long id,
        @CurrentUsername String username) {
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
        AccountDTO account = accountService.getAccountByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(account.getBankAccounts()));
    }
}
