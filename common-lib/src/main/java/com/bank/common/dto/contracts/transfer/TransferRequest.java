package com.bank.common.dto.contracts.transfer;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequest {
    @NotNull(message = "From bank account ID is required")
    private Long fromBankAccountId;

    // Either toBankAccountId or recipientEmail must be provided
    private Long toBankAccountId;

    @Email(message = "Invalid email format")
    private String recipientEmail;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Double amount;
}
