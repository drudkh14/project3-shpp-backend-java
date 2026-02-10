package com.zhbohdanchykov;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = CheckEddrValidator.class)
public @interface CheckEddr {
    String message() default "Invalid EDDR number.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
