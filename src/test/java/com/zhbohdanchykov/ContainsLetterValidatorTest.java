package com.zhbohdanchykov;

import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class ContainsLetterValidatorTest {

    private ContainsLetterValidator validator;
    private ContainsLetter annotation;
    private ConstraintValidatorContext context;

    @BeforeEach
    public void setUp() {
        validator = new ContainsLetterValidator();
        annotation = Mockito.mock(ContainsLetter.class);
        context = Mockito.mock(ConstraintValidatorContext.class);
    }

    private void init(char c) {
        Mockito.when(annotation.value()).thenReturn(c);
        validator.initialize(annotation);
    }

    @Test
    void containsLetterSameCase() {
        init('a');
        Assertions.assertTrue(validator.isValid("alex", context));
    }

    @Test
    void containsUppercaseLetterValueLowercase() {
        init('b');
        Assertions.assertTrue(validator.isValid("Barry", context));
    }

    @Test
    void containsLowercaseLetterValueUppercase() {
        init('A');
        Assertions.assertTrue(validator.isValid("alex", context));
    }

    @Test
    void valueIsNotLetter() {
        Mockito.when(annotation.value()).thenReturn('7');
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> validator.initialize(annotation));
    }

    @Test
    void doesNotContainLetter() {
        init('g');
        Assertions.assertFalse(validator.isValid("Kate", context));
    }

    @Test
    void emptyString() {
        init('a');
        Assertions.assertFalse(validator.isValid("", context));
    }

    @Test
    void stringContainsDifferentChars() {
        init('a');
        Assertions.assertFalse(validator.isValid("!@#$%^&*()_+=1234", context));
    }

    @Test
    void nullString() {
        init('a');
        Assertions.assertTrue(validator.isValid(null, context));
    }
}