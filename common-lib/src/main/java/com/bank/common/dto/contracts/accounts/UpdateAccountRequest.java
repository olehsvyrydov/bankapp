package com.bank.common.dto.contracts.accounts;

import com.bank.common.validation.MinAge;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAccountRequest {

    @NotBlank(message = "{validation.firstName.required}")
    @Size(min = 2, max = 50, message = "{validation.firstName.length}")
    private String firstName;

    @NotBlank(message = "{validation.lastName.required}")
    @Size(min = 2, max = 50, message = "{validation.lastName.length}")
    private String lastName;

    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    @NotNull(message = "{validation.birthDate.required}")
    @Past(message = "{validation.birthDate.past}")
    @MinAge(value = 18, message = "{validation.minAge}")
    private LocalDate birthDate;
}
