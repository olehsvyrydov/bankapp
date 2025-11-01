
package com.bank.common.dto.contracts.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public record RegisterRequest(
    @NotBlank(message = "{validation.username.required}")
    @Size(min = 3, max = 20, message = "{validation.username.lengthShort}")
    String username,

    @NotBlank(message = "{validation.firstName.required}")
    @Size(min = 3, max = 20, message = "{validation.firstName.lengthShort}")
    String firstName,

    @NotBlank(message = "{validation.lastName.required}")
    @Size(min = 3, max = 50, message = "{validation.lastName.lengthLong}")
    String lastName,

    @NotBlank(message = "{validation.password.required}")
    @Size(min = 6, message = "{validation.password.minLength}")
    String password,

    @NotBlank(message = "{validation.confirmPassword.required}")
    @Size(min = 6, message = "{validation.password.minLength}")
    String confirmPassword,

    @Email(message = "{validation.email.invalid}")
    @NotBlank(message = "{validation.email.required}")
    String email,

    @Past(message = "{validation.birthDate.past}")
    LocalDate birthDate
)
{
    public static RegisterRequest NULL_REQUEST = new RegisterRequest("", "", "", "", "", "", LocalDate.EPOCH);
}
