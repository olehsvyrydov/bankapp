package com.bank.common.dto.contracts.cash;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashOperationRequest {

    @NotNull(message = "{validation.bankAccountId.required}")
    private Long bankAccountId;

    @NotNull(message = "{validation.amount.required}")
    @Positive(message = "{validation.amount.positive}")
    private BigDecimal amount;

    private String type; // DEPOSIT or WITHDRAWAL
}
