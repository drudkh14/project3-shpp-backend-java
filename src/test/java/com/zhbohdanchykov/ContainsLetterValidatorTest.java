package com.zhbohdanchykov;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import junit.framework.TestCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class ContainsLetterValidatorTest extends TestCase {

    private Validator validator;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void containsLetter() {
        MessagePOJO message1 = new MessagePOJO("Alice", "19700101-99991", 000, LocalDateTime.of(2000, 01, 01, 00, 00));
    }
}