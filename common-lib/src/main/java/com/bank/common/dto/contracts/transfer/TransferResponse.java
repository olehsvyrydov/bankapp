package com.bank.common.dto.contracts.transfer;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResponse {
    private Long transferId;
    private String status;
    private String message;
    private BigDecimal convertedAmount;
}
