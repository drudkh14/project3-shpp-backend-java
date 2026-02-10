package com.zhbohdanchykov;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

/**
 * The class PropertiesLoader is supposed to load properties from given properties file.
 */
public class PropertiesLoader {

    private final String fileName;

    private static final Logger logger = LoggerFactory.getLogger(PropertiesLoader.class);

    /**
     * Constructs PropertiesLoader with given properties file
     *
     * @param fileName Name of properties file
     */
    public PropertiesLoader(String fileName) {
        this.fileName = fileName;
        logger.info("Created PropertiesLoader. Got file {} for properties", fileName);
    }

    /**
     * Loads properties
     *
     * @return Properties
     * @throws IOException to throw if there is a problem with file reading
     */
    public Properties loadProperties() throws IOException {
        Properties properties = new Properties();

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(fileName);

        if (inputStream != null) {
            logger.debug("Passing InputStream to InputStreamReader");
            try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                logger.debug("Loading properties with InputStreamReader");
                properties.load(reader);
            }
        } else {
            logger.debug("InputStream is null.");
            throw new FileNotFoundException("File not found: " + fileName);
        }

        logger.info("Loaded properties {} from {}", properties, fileName);

        try {
            allPropertiesAvailable(properties);
        } catch (InvalidPropertiesFormatException e) {
            throw new InvalidPropertiesFormatException(e.getMessage());
        }

        return properties;
    }

    private void allPropertiesAvailable(Properties properties) throws InvalidPropertiesFormatException {
        if (properties.getProperty("url") == null || properties.getProperty("url").isEmpty()) {
            throw new InvalidPropertiesFormatException("Broker's url is missing.");
        }

        if (properties.getProperty("queue") == null || properties.getProperty("queue").isEmpty()) {
            throw new InvalidPropertiesFormatException("Queue is missing.");
        }

        if (properties.getProperty("stop") == null || properties.getProperty("stop").isEmpty()) {
            throw new InvalidPropertiesFormatException("Stop is missing.");
        }
    }
}