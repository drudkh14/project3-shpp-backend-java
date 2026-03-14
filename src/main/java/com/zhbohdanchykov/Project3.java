package com.zhbohdanchykov;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

public class Project3 {

    private static final Logger LOGGER = LoggerFactory.getLogger(Project3.class);
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final String PROPERTIES_FILENAME = "project3.properties";

    public static void main(String[] args) {
        ProjectProperties properties;
        try {
            properties = new ProjectProperties(PROPERTIES_FILENAME);
        } catch (IOException | NumberFormatException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        Set<ConstraintViolation<ProjectProperties>> violations = VALIDATOR.validate(properties);
        if (!violations.isEmpty()) {
            LOGGER.error("Properties validation failed. Validation errors: {}",
                    violations.stream().map(ConstraintViolation::getMessage).toList());
            return;
        }

        new Application(properties).start();
    }
}
