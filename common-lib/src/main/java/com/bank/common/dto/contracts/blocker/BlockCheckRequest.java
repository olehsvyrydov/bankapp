package com.bank.common.dto.contracts.blocker;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BlockCheckRequest {
    private String username;
    private Double amount;
    private String type;
}
