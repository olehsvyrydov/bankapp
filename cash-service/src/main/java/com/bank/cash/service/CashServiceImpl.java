package com.bank.cash.service;

import com.bank.cash.client.AccountsClient;
import com.bank.cash.client.BlockerClient;
import com.bank.cash.client.NotificationClient;
import com.bank.common.dto.contracts.cash.CashOperationRequest;
import com.bank.common.dto.contracts.cash.CashOperationResponse;
import com.bank.common.dto.contracts.notifications.NotificationRequest;
import com.bank.cash.entity.Transaction;
import com.bank.cash.repository.TransactionRepository;
import com.bank.common.dto.ApiResponse;
import com.bank.common.exception.BusinessException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class CashServiceImpl implements CashService
{
    private final TransactionRepository transactionRepository;
    private final AccountsClient accountsClient;
    private final BlockerClient blockerClient;
    private final NotificationClient notificationClient;
    private final ObjectMapper objectMapper;

    public CashServiceImpl(TransactionRepository transactionRepository,
        AccountsClient accountsClient,
        BlockerClient blockerClient,
        NotificationClient notificationClient,
        ObjectMapper objectMapper)
    {
        this.transactionRepository = transactionRepository;
        this.accountsClient = accountsClient;
        this.blockerClient = blockerClient;
        this.notificationClient = notificationClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public CashOperationResponse processOperation(CashOperationRequest request, String username)
    {
        try {
            Map<String, Object> bankAccount = fetchBankAccount(request.getBankAccountId());

            String currency = (String) bankAccount.get("currency");
            double currentBalance = ((Number) bankAccount.get("balance")).doubleValue();

            // Check with blocker
            Map<String, Object> blockerRequest = new HashMap<>();
            blockerRequest.put("username", username);
            blockerRequest.put("amount", request.getAmount());
            blockerRequest.put("type", request.getType());
            log.debug("Blocker request: {}", blockerRequest);
            Map<String, Object> blockerResponse = blockerClient.checkOperation(blockerRequest);
            boolean isBlocked = Boolean.TRUE.equals(blockerResponse.get("blocked"));
            log.debug("Blocker response: {}", blockerResponse);
            if (isBlocked)
            {
                Transaction transaction = Transaction.builder()
                    .bankAccountId(request.getBankAccountId())
                    .type(request.getType())
                    .amount(request.getAmount())
                    .currency(currency)
                    .status("BLOCKED")
                    .description("Operation blocked by security system")
                    .build();
                transactionRepository.save(transaction);

                notificationClient.sendNotification(NotificationRequest.builder()
                    .username(username)
                    .message("Suspicious operation blocked: " + request.getType() + " " + request.getAmount())
                    .type("WARNING")
                    .build());

                throw new BusinessException("Operation blocked by security system");
            }

            // Validate withdrawal
            if (request.getType() != null && request.getType().toUpperCase().contains("WITHDRAW")
                && currentBalance < request.getAmount())
            {
                throw new BusinessException("Insufficient balance");
            }

            // Update balance
            String operation = "DEPOSIT".equalsIgnoreCase(request.getType()) ? "ADD" : "SUBTRACT";
            Map<String, Object> updateRequest = new HashMap<>();
            updateRequest.put("bankAccountId", request.getBankAccountId());
            updateRequest.put("amount", request.getAmount());
            updateRequest.put("operation", operation);

            Map<String, Object> updatedAccount = updateBalance(updateRequest, operation);
            Double newBalance = ((Number) updatedAccount.get("balance")).doubleValue();

            // Save transaction
            Transaction transaction = Transaction.builder()
                .bankAccountId(request.getBankAccountId())
                .type(request.getType())
                .amount(request.getAmount())
                .currency(currency)
                .status("SUCCESS")
                .description(request.getType() + " completed successfully")
                .build();
            transaction = transactionRepository.save(transaction);

            // Send notification
            notificationClient.sendNotification(NotificationRequest.builder()
                .username(username)
                .message(request.getType() + " of " + request.getAmount() + " " + currency + " completed")
                .type("INFO")
                .build());

            return CashOperationResponse.builder()
                .transactionId(transaction.getId())
                .status("SUCCESS")
                .message(request.getType() + " completed successfully")
                .newBalance(newBalance)
                .build();
        } catch (BusinessException ex) {
            log.error("Cash operation failed: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected cash operation failure", ex);
            throw new BusinessException("Cash operation failed: " + ex.getMessage());
        }
    }

    private Map<String, Object> fetchBankAccount(Long bankAccountId)
    {
        try
        {
            ApiResponse<Map<String, Object>> response = accountsClient.getBankAccount(bankAccountId);
            if (!response.isSuccess())
            {
                throw new BusinessException(response.getMessage());
            }
            Map<String, Object> bankAccount = response.getData();
            if (bankAccount == null)
            {
                throw new BusinessException("Bank account not found");
            }
            return bankAccount;
        }
        catch (FeignException ex)
        {
            throw mapFeignException("Failed to load bank account", ex);
        }
    }

    private Map<String, Object> updateBalance(Map<String, Object> request, String operation)
    {
        try
        {
            ApiResponse<Map<String, Object>> response = accountsClient.updateBalance(request);
            if (!response.isSuccess())
            {
                throw new BusinessException("Failed to update balance: " + response.getMessage());
            }
            return response.getData();
        }
        catch (FeignException ex)
        {
            throw mapFeignException("Failed to update balance (" + operation + ")", ex);
        }
    }

    private BusinessException mapFeignException(String action, FeignException ex)
    {
        String message = action;
        if (ex.responseBody().isPresent())
        {
            try
            {
                JsonNode node = objectMapper.readTree(ex.contentUTF8());
                if (node.has("message"))
                {
                    message = node.get("message").asText();
                }
            }
            catch (Exception ignored)
            {
            }
        }
        return new BusinessException(message);
    }
}
