package com.zhbohdanchykov;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class Project3 {

    private static final Logger LOGGER = LoggerFactory.getLogger(Project3.class);

    private static final String PROPERTIES_FILENAME = "project3.properties";

    public static void main(String[] args) {
        LOGGER.info("Starting Main");
        ProjectProperties properties;
        try {
            properties = new ProjectProperties(PROPERTIES_FILENAME);
        } catch (IOException | NumberFormatException e) {
            LOGGER.error(e.getMessage(), e);
            return;
        }

        Validator validator;
        try(ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        Set<ConstraintViolation<ProjectProperties>> violations = validator.validate(properties);
        if (!violations.isEmpty()) {
            LOGGER.error("Properties validation failed. Validation errors: {}",
                    violations.stream().map(ConstraintViolation::getMessage).toList());
            return;
        }

        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(properties.getUrl());
        connectionFactory.setTrustedPackages(List.of("com.zhbohdanchykov.MessagePOJO"));

        new Application(properties, validator, connectionFactory).start();
        LOGGER.info("Finished Main");
    }
}
