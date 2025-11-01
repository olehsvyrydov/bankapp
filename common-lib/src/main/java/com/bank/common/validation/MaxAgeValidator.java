package com.bank.common.validation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;
/**
 * Validator for @MaxAge annotation.
 * Validates that the age calculated from the date is not greater than the maximum.
 */
public class MaxAgeValidator implements ConstraintValidator<MaxAge, LocalDate> {
    private int maxAge;
    @Override
    public void initialize(MaxAge constraintAnnotation) {
        this.maxAge = constraintAnnotation.value();
    }
    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return true; // null values are handled by @NotNull
        }
        LocalDate currentDate = LocalDate.now();
        // Birth date must be in the past
        if (birthDate.isAfter(currentDate)) {
            return false;
        }
        int age = Period.between(birthDate, currentDate).getYears();
        return age <= maxAge;
    }
}
