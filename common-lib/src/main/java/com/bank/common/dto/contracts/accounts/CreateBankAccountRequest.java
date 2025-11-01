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

    @NotBlank(message = "{validation.currency.required}")
    @Pattern(regexp = "RUB|USD|CNY", message = "{validation.currency.allowed}")
    private String currency;
}
