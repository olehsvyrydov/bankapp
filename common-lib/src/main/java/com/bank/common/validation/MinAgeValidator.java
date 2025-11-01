package com.bank.common.validation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;
/**
 * Validator for @MinAge annotation.
 * Validates that the age calculated from the date is at least the minimum required.
 */
public class MinAgeValidator implements ConstraintValidator<MinAge, LocalDate> {
    private int minAge;
    @Override
    public void initialize(MinAge constraintAnnotation) {
        this.minAge = constraintAnnotation.value();
    }
    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return true; // null values are handled by @NotNull
        }
        LocalDate currentDate = LocalDate.now();
        // Birth date must be in the past
        if (birthDate.isAfter(currentDate) || birthDate.isEqual(currentDate)) {
            return false;
        }
        int age = Period.between(birthDate, currentDate).getYears();
        return age >= minAge;
    }
}
