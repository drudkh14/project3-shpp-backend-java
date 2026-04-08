package com.zhbohdanchykov;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = ContainsLetterValidator.class)
public @interface ContainsLetter {
    char value();

    String message() default "Name must contain letter \"{value}\".";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
