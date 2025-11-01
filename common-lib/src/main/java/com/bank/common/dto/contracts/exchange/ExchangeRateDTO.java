package com.bank.common.dto.contracts.exchange;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExchangeRateDTO {
    private String currency;
    private Double buyRate;
    private Double sellRate;
}
