package com.bank.common.dto.contracts.exchange;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionRequest {
    private BigDecimal amount;
    private String fromCurrency;
    private String toCurrency;
}
