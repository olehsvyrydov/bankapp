package com.bank.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Validates that the annotated date field represents an age that is not greater than the specified maximum.
 * The age is calculated in years from the annotated date to the current date.
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = MaxAgeValidator.class)
@Documented
public @interface MaxAge {
    String message() default "{validation.maxAge}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    /**
     * @return the maximum age in years
     */
    int value();
}
