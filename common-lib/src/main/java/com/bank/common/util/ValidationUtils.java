package com.bank.common.util;

import com.bank.common.exception.ValidationException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

public class ValidationUtils {
    private ValidationUtils() {}

    public static void validateAge(LocalDate birthDate, int minAge) {
        if (birthDate == null) {
            throw new ValidationException("Birth date is required",
                Map.of("birthDate", "Birth date cannot be null"));
        }

        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age < minAge) {
            throw new ValidationException("Age requirement not met",
                Map.of("birthDate", "User must be at least " + minAge + " years old"));
        }
    }

    public static void validateNotEmpty(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException("Field cannot be empty",
                Map.of(fieldName, fieldName + " is required"));
        }
    }

    public static void validatePositiveAmount(Double amount) {
        if (amount == null || amount <= 0) {
            throw new ValidationException("Invalid amount",
                Map.of("amount", "Amount must be positive"));
        }
    }

    public static void validateEmail(String email) {
        if (email != null && !email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new ValidationException("Invalid email format",
                Map.of("email", "Please provide a valid email address"));
        }
    }

    public static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            throw new ValidationException("Weak password",
                Map.of("password", "Password must be at least 6 characters long"));
        }
    }
}
