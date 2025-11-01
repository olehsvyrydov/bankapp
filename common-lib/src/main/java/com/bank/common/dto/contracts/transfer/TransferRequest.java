package com.bank.common.dto.contracts.transfer;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotNull(message = "{validation.transfer.from.required}")
    private Long fromBankAccountId;

    // Either toBankAccountId or recipientEmail must be provided
    private Long toBankAccountId;

    @Email(message = "{validation.email.invalid}")
    private String recipientEmail;

    @NotNull(message = "{validation.amount.required}")
    @Positive(message = "{validation.amount.positive}")
    private Double amount;
}
