package com.bank.common.dto.contracts.accounts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBankAccountRequest {

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "RUB|USD|CNY", message = "Currency must be RUB, USD, or CNY")
    private String currency;
}
