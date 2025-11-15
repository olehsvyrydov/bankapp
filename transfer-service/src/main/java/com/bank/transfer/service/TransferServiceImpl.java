package com.bank.transfer.service;

import com.bank.common.dto.contracts.accounts.BankAccountDTO;
import com.bank.common.dto.contracts.accounts.UpdateBalanceRequest;
import com.bank.common.dto.contracts.blocker.BlockCheckRequest;
import com.bank.common.dto.contracts.blocker.BlockCheckResponse;
import com.bank.common.dto.contracts.exchange.ConversionRequest;
import com.bank.common.dto.contracts.notifications.NotificationRequest;
import com.bank.common.dto.contracts.transfer.TransferRequest;
import com.bank.common.dto.contracts.transfer.TransferResponse;
import com.bank.transfer.client.*;
import com.bank.transfer.kafka.NotificationProducer;
import com.bank.transfer.entity.Transfer;
import com.bank.transfer.repository.TransferRepository;
import com.bank.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.bank.common.dto.contracts.accounts.BankOperation.ADD;
import static com.bank.common.dto.contracts.accounts.BankOperation.SUBTRACT;

@Service
@Transactional
@Slf4j
public class TransferServiceImpl implements TransferService {

    private final TransferRepository transferRepository;
    private final AccountsClient accountsClient;
    private final ExchangeClient exchangeClient;
    private final BlockerClient blockerClient;
    private final NotificationProducer notificationProducer;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public TransferServiceImpl(TransferRepository transferRepository,
        AccountsClient accountsClient,
        ExchangeClient exchangeClient,
        BlockerClient blockerClient,
        NotificationProducer notificationProducer) {
        this.transferRepository = transferRepository;
        this.accountsClient = accountsClient;
        this.exchangeClient = exchangeClient;
        this.blockerClient = blockerClient;
        this.notificationProducer = notificationProducer;
    }

    @Override
    public TransferResponse processTransfer(TransferRequest request, String username) {
        try {
            // Validate that either toBankAccountId or recipientEmail is provided
            if (request.getToBankAccountId() == null &&
                (request.getRecipientEmail() == null || request.getRecipientEmail().isBlank())) {
                throw new BusinessException("Either destination bank account ID or recipient email must be provided");
            }

            // Get from account
            BankAccountDTO fromAccount = accountsClient.getBankAccountById(
                request.getFromBankAccountId()).getData();

            if (fromAccount == null) {
                throw new BusinessException("Source bank account not found");
            }

            if (!username.equals(fromAccount.getAccountUsername())) {
                throw new BusinessException("Source bank account does not belong to the user");
            }

            String fromCurrency = fromAccount.getCurrency();
            BigDecimal fromBalance = fromAccount.getBalance();

            // Check balance
            if (fromBalance.compareTo(request.getAmount()) < 0) {
                throw new BusinessException("Insufficient balance");
            }

            // Get destination account (by ID or email)
            BankAccountDTO toAccount = null;
            if (request.getToBankAccountId() != null) {
                toAccount = accountsClient.getBankAccountById(request.getToBankAccountId()).getData();
                if (toAccount == null) {
                    throw new BusinessException("Destination bank account not found");
                }
            } else if (request.getRecipientEmail() != null && !request.getRecipientEmail().isBlank()) {
                // Find user by email and get their first bank account with matching currency
                var response = accountsClient.getBankAccountsByEmail(request.getRecipientEmail());
                if (response == null || !response.isSuccess() || response.getData() == null || response.getData().isEmpty()) {
                    throw new BusinessException("No user found with email: " + request.getRecipientEmail());
                }

                List<BankAccountDTO> recipientAccounts = response.getData();

                // Try to find account with same currency first
                toAccount = recipientAccounts.stream()
                    .filter(acc -> acc.getCurrency().equals(fromCurrency))
                    .findFirst()
                    .orElse(null);

                // If no matching currency found, use first available account with conversion
                if (toAccount == null) {
                    toAccount = recipientAccounts.get(0);
                    log.info("No {} account found for recipient {}, using {} account with currency conversion",
                        fromCurrency, request.getRecipientEmail(), toAccount.getCurrency());
                } else {
                    log.info("Found matching {} account for recipient {}", fromCurrency, request.getRecipientEmail());
                }

                // Update request with found account ID
                request.setToBankAccountId(toAccount.getId());
            }

            if (toAccount == null) {
                throw new BusinessException("Destination bank account not found");
            }

            String toCurrency = toAccount.getCurrency();
            String toUsername = toAccount.getAccountUsername();

            log.info("Transfer details - From: {} {}, To: {} {}, Amount: {}",
                fromCurrency, fromBalance, toCurrency, toAccount.getBalance(), request.getAmount());

            // Check with blocker ONLY for transfers to other users' accounts
            // Don't block transfers between user's own accounts
            boolean isOwnAccountTransfer = username.equals(toUsername);

            if (!isOwnAccountTransfer) {
                log.info("Checking blocker for transfer from user {} to other user {}", username, toUsername);
                BlockCheckResponse blockedResponse = blockerClient.checkOperation(
                    BlockCheckRequest.builder()
                        .username(username)
                        .amount(request.getAmount())
                        .type("TRANSFER")
                        .build());

                if (blockedResponse.blocked()) {
                    Transfer transfer = Transfer.builder()
                        .amount(request.getAmount())
                        .fromBankAccountId(request.getFromBankAccountId())
                        .toBankAccountId(request.getToBankAccountId())
                        .fromCurrency(fromCurrency)
                        .toCurrency(toCurrency)
                        .status("BLOCKED")
                        .description("The operation looks suspicious and is blocked by bank")
                        .build();
                    transferRepository.save(transfer);

                    notificationProducer.sendNotification(NotificationRequest.builder()
                        .username(username)
                        .message("The operation looks suspicious and is blocked by bank")
                        .type("WARNING")
                        .build());
                    throw new BusinessException("The operation looks suspicious and is blocked by bank");
                }
            } else {
                log.info("Skipping blocker check - transfer between own accounts for user {}", username);
            }

            // Convert currency if needed
            BigDecimal convertedAmount = request.getAmount();
            if (!fromCurrency.equals(toCurrency)) {
                log.info("Currency conversion needed: {} {} -> {}", request.getAmount(), fromCurrency, toCurrency);
                var resp = exchangeClient.convert(
                    ConversionRequest.builder()
                        .amount(request.getAmount())
                        .fromCurrency(fromCurrency)
                        .toCurrency(toCurrency)
                        .build());
                if (resp.isSuccess())
                {
                    convertedAmount = resp.getData();
                    log.info("Currency converted: {} {} -> {} {}", request.getAmount(), fromCurrency,
                        convertedAmount, toCurrency);
                } else {
                    throw new BusinessException("Currency conversion failed: " + resp.getMessage());
                }
            } else {
                log.info("No currency conversion needed: both accounts use {}", fromCurrency);
            }

            // Update balances
            log.info("Updating balances: Subtracting {} {} from account {}, Adding {} {} to account {}",
                request.getAmount(), fromCurrency, request.getFromBankAccountId(),
                convertedAmount, toCurrency, request.getToBankAccountId());

            accountsClient.updateBalance(
                UpdateBalanceRequest.builder()
                    .bankAccountId(request.getFromBankAccountId())
                    .amount(request.getAmount())
                    .operation(SUBTRACT)
                    .build());
            accountsClient.updateBalance(
                UpdateBalanceRequest.builder()
                    .bankAccountId(request.getToBankAccountId())
                    .amount(convertedAmount)
                    .operation(ADD)
                    .build());

            // Save transfer
            Transfer transfer = Transfer.builder()
                .fromBankAccountId(request.getFromBankAccountId())
                .toBankAccountId(request.getToBankAccountId())
                .amount(request.getAmount())
                .fromCurrency(fromCurrency)
                .toCurrency(toCurrency)
                .convertedAmount(convertedAmount)
                .status("SUCCESS")
                .description("Transfer completed successfully")
                .build();
            transfer = transferRepository.save(transfer);

            // Send notifications via Kafka
            notificationProducer.sendNotification(NotificationRequest.builder()
                .username(username)
                .message("Transfer of " + request.getAmount() + " " + fromCurrency + " sent")
                .type("INFO")
                .build());

            if (!username.equals(toUsername)) {
                notificationProducer.sendNotification(NotificationRequest.builder()
                    .username(toUsername)
                    .message("Transfer of " + convertedAmount + " " + toCurrency + " received")
                    .type("INFO")
                    .build());
            }

            return TransferResponse.builder()
                .transferId(transfer.getId())
                .status("SUCCESS")
                .message("Transfer completed successfully")
                .convertedAmount(convertedAmount)
                .build();
        } catch (FeignException ex) {
            throw new BusinessException(resolveFeignMessage("Accounts service error", ex));
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected transfer failure", ex);
            throw new BusinessException("Transfer failed: " + ex.getMessage());
        }
    }

    private String resolveFeignMessage(String defaultMessage, FeignException ex) {
        if (ex.responseBody().isPresent()) {
            try {
                JsonNode node = OBJECT_MAPPER.readTree(ex.contentUTF8());
                if (node.has("message")) {
                    return node.get("message").asText();
                }
            } catch (Exception ignored) {
            }
        }
        return defaultMessage;
    }
}
