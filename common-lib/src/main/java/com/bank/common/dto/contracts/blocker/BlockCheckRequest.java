package com.bank.common.dto.contracts.blocker;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockCheckRequest {
    private String username;
    private BigDecimal amount;
    private String type;
}
