package com.bank.common.dto.contracts.exchange;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDTO {
    private String currency;
    private BigDecimal buyRate;
    private BigDecimal sellRate;
}
