package com.bank.common.dto.contracts.accounts;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBalanceRequest {
    private Long bankAccountId;
    private BigDecimal amount;
    @NotNull(message = "Operation is required")
    private BankOperation operation;
}
