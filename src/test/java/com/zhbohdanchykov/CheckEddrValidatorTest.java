package com.zhbohdanchykov;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;

class CheckEddrValidatorTest {

    private CheckEddrValidator validator;
    private ConstraintValidatorContext context;

    @BeforeEach
    void setUp() {
        validator = new CheckEddrValidator();
        CheckEddr annotation = mock(CheckEddr.class);
        context = mock(ConstraintValidatorContext.class);
        validator.initialize(annotation);
    }

    @Test
    void validEddr() {
        Assertions.assertTrue(validator.isValid("19910824-00026", context));
    }

    @Test
    void invalidLastDigitEddr() {
        Assertions.assertFalse(validator.isValid("19910824-00025", context));
    }

    @Test
    void noMinus() {
        Assertions.assertFalse(validator.isValid("1991082400026", context));
    }

    @Test
    void shortEddr() {
        Assertions.assertFalse(validator.isValid("19910824-0002", context));
    }

    @Test
    void longEddr() {
        Assertions.assertFalse(validator.isValid("19910824-000265", context));
    }

    @Test
    void onlySpaces() {
        Assertions.assertFalse(validator.isValid("              ", context));
    }

    @Test
    void leadingSpaces() {
        Assertions.assertFalse(validator.isValid("   19910824-00026", context));
    }

    @Test
    void trailingSpaces() {
        Assertions.assertFalse(validator.isValid("19910824-00026   ", context));
    }

    @Test
    void correctLengthWithSpaces() {
        Assertions.assertFalse(validator.isValid("19910824-0002 ", context));
    }

    @Test
    void wrongDashPosition() {
        Assertions.assertFalse(validator.isValid("1991082-400026", context));
    }

    @Test
    void letterInPrefix() {
        Assertions.assertFalse(validator.isValid("1991082F-00026", context));
    }

    @Test
    void letterInSuffix() {
        Assertions.assertFalse(validator.isValid("19910824-00A26", context));
    }

    @Test
    void specialCharacter() {
        Assertions.assertFalse(validator.isValid("19910824-00@26", context));
    }
}