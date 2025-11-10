package com.bank.common.dto.contracts.accounts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDTO {
    private Long id;
    private String currency;
    private BigDecimal balance;
    private String accountUsername;
}
