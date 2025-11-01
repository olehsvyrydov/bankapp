package com.bank.common.dto.contracts.accounts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBalanceRequest {
    private Long bankAccountId;
    private Double amount;
    private String operation;
}
