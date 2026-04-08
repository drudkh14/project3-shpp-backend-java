package com.zhbohdanchykov;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ContainsLetterValidator implements ConstraintValidator<ContainsLetter, String> {

    private char value;

    @Override
    public void initialize(ContainsLetter constraintAnnotation) {
        value = constraintAnnotation.value();
        if (!Character.isLetter(value)) {
            throw new IllegalArgumentException("Value must be a letter");
        }
    }

    @Override
    public boolean isValid(String object, ConstraintValidatorContext context) {
        if (object == null) {
            return true;
        }
        return object.toLowerCase().
                contains(String.valueOf(value).toLowerCase());
    }
}
