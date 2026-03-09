package com.zhbohdanchykov;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Properties;

public class ProjectProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectProperties.class);

    private final String url;
    private final String queueName;
    private final int stopTime;

    public ProjectProperties(String filename) throws IOException, NumberFormatException {
        LOGGER.info("Created ProjectProperties. Got file {} for properties. Passing to PropertiesLoader", filename);
        Properties properties = new PropertiesLoader(filename).loadProperties();
        this.url = properties.getProperty("url");
        this.queueName = properties.getProperty("queue");
        this.stopTime = Integer.parseInt(properties.getProperty("stop"));
    }

    @NotNull(message = "URL must not be null.")
    @NotBlank(message = "URL must not be blank.")
    public String getUrl() {
        return url;
    }

    @NotNull(message = "Name of queue must not be null.")
    @NotBlank(message = "Name of queue must not be blank.")
    public String getQueueName() {
        return queueName;
    }

    @Min(value = 0, message = "Stop time must be positive.")
    public int getStopTime() {
        return stopTime;
    }
}
