package com.bank.common.dto.contracts.cash;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashOperationResponse {
    private Long transactionId;
    private String status;
    private String message;
    private BigDecimal newBalance;
}
