package com.zhbohdanchykov;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class CheckEddrValidator implements ConstraintValidator<CheckEddr, String> {

    public static final int VALID_EDDR_LENGTH = 14;
    public static final int VALID_EDDR_MINUS_POS = 8;
    public static final int[] VALID_EDDR_WEIGHTS = {7, 3, 1};

    @Override
    public void initialize(CheckEddr constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
    }

    @Override
    public boolean isValid(String eddr, ConstraintValidatorContext context) {
        if (eddr == null) {
            return true;
        }

        if (!eddr.contains("-"))
            return false;
        if (!(eddr.length() == VALID_EDDR_LENGTH))
            return false;
        if (eddr.charAt(VALID_EDDR_MINUS_POS) != '-')
            return false;
        eddr = eddr.replace("-", "");
        String lastDigit = String.valueOf(eddr.charAt(eddr.length() - 1));
        int sum = 0;
        for (int i = 0; i < eddr.length() - 1; i++) {
            try {
                sum += Integer.parseInt(String.valueOf(eddr.charAt(i))) *
                        VALID_EDDR_WEIGHTS[i % VALID_EDDR_WEIGHTS.length];
            } catch (NumberFormatException e) {
                return false;
            }
        }
        int controlDigit = sum % 10;
        return lastDigit.equals(String.valueOf(controlDigit));
    }
}
