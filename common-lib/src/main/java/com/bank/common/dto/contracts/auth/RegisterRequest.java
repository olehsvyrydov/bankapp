
package com.bank.common.dto.contracts.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public record RegisterRequest(
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20)
    String username,

    @NotBlank(message = "First name is required")
    @Size(min = 3, max = 20)
    String firstName,

    @NotBlank(message = "Last name is required")
    @Size(min = 3, max = 50)
    String lastName,

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String password,

    @NotBlank(message = "Confirm Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    String confirmPassword,

    @Email(message = "Invalid email format")
    @NotBlank(message = "Email is required")
    String email,

    @Past(message = "Birth date must be in the past")
    LocalDate birthDate
)
{
    public static RegisterRequest NULL_REQUEST = new RegisterRequest("", "", "", "", "", "", LocalDate.EPOCH);
}
