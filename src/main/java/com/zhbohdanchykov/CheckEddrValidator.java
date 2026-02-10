package com.zhbohdanchykov;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CheckEddrValidator implements ConstraintValidator<CheckEddr, String> {

    @Override
    public void initialize(CheckEddr constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String eddr, ConstraintValidatorContext context) {
        if (eddr == null) {
            return true;
        }

        eddr = eddr.replace("-", "");
        String lastDigit = String.valueOf(eddr.charAt(eddr.length() - 1));
        int[] weights = new int[]{7, 3, 1};
        int sum = 0;
        for (int i = 0; i < eddr.length() - 1; i++) {
            sum += Integer.parseInt(String.valueOf(eddr.charAt(i))) * weights[i % weights.length];
        }
        int controlDigit = sum % 10;
        return lastDigit.equals(String.valueOf(controlDigit));
    }
}
