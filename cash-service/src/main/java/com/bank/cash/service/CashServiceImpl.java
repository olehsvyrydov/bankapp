package com.bank.cash.service;

import com.bank.cash.client.AccountsClient;
import com.bank.cash.client.BlockerClient;
import com.bank.cash.kafka.NotificationProducer;
import com.bank.common.dto.contracts.accounts.BankAccountDTO;
import com.bank.common.dto.contracts.accounts.BankOperation;
import com.bank.common.dto.contracts.accounts.UpdateBalanceRequest;
import com.bank.common.dto.contracts.blocker.BlockCheckRequest;
import com.bank.common.dto.contracts.blocker.BlockCheckResponse;
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
import java.math.BigDecimal;

import static com.bank.common.dto.contracts.accounts.BankOperation.ADD;
import static com.bank.common.dto.contracts.accounts.BankOperation.SUBTRACT;

@Service
@Transactional
@Slf4j
public class CashServiceImpl implements CashService
{
    private final TransactionRepository transactionRepository;
    private final AccountsClient accountsClient;
    private final BlockerClient blockerClient;
    private final NotificationProducer notificationProducer;
    private final ObjectMapper objectMapper;

    public CashServiceImpl(TransactionRepository transactionRepository,
        AccountsClient accountsClient,
        BlockerClient blockerClient,
        NotificationProducer notificationProducer,
        ObjectMapper objectMapper)
    {
        this.transactionRepository = transactionRepository;
        this.accountsClient = accountsClient;
        this.blockerClient = blockerClient;
        this.notificationProducer = notificationProducer;
        this.objectMapper = objectMapper;
    }

    @Override
    public CashOperationResponse processOperation(CashOperationRequest request, String username)
    {
        try {
            BankAccountDTO bankAccount = fetchBankAccount(request.getBankAccountId());

            String currency = bankAccount.getCurrency();
            BigDecimal currentBalance = bankAccount.getBalance();

            // Check with blocker
            BlockCheckRequest blockerRequest = BlockCheckRequest.builder()
                .username(username)
                .amount(request.getAmount())
                .type(request.getType())
                .build();
            log.debug("Blocker request: {}", blockerRequest);
            BlockCheckResponse blockerResponse = blockerClient.checkOperation(blockerRequest);
            boolean isBlocked = blockerResponse.blocked();
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

                notificationProducer.sendNotification(NotificationRequest.builder()
                    .username(username)
                    .message("Suspicious operation blocked: " + request.getType() + " " + request.getAmount())
                    .type("WARNING")
                    .build());

                throw new BusinessException("Operation blocked by security system");
            }

            // Validate withdrawal
            if (request.getType() != null && request.getType().toUpperCase().contains("WITHDRAW")
                && currentBalance.compareTo(request.getAmount()) < 0)
            {
                throw new BusinessException("Insufficient balance");
            }

            // Update balance
            BankOperation operation = "DEPOSIT".equalsIgnoreCase(request.getType()) ? ADD : SUBTRACT;
            UpdateBalanceRequest updateRequest = UpdateBalanceRequest.builder()
                .bankAccountId(request.getBankAccountId())
                .amount(request.getAmount())
                .operation(operation)
                .build();

            BankAccountDTO updatedAccount = updateBalance(updateRequest, operation);
            BigDecimal newBalance = updatedAccount.getBalance();

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

            // Send notification via Kafka
            notificationProducer.sendNotification(NotificationRequest.builder()
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

    private BankAccountDTO fetchBankAccount(Long bankAccountId)
    {
        try
        {
            ApiResponse<BankAccountDTO> response = accountsClient.getBankAccount(bankAccountId);
            if (!response.isSuccess())
            {
                throw new BusinessException(response.getMessage());
            }
            BankAccountDTO bankAccount = response.getData();
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

    private BankAccountDTO updateBalance(UpdateBalanceRequest request, BankOperation operation)
    {
        try
        {
            ApiResponse<BankAccountDTO> response = accountsClient.updateBalance(request);
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
                // Ignore parsing errors
            }
        }
        return new BusinessException(message);
    }
}
