package com.bank.common.dto.contracts.exchange;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionRequest {
    private Double amount;
    private String fromCurrency;
    private String toCurrency;
}
