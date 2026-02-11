package com.zhbohdanchykov;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Project3 {
    public static final int THREADS_NUMBER = 10;
    private static final Logger logger = LoggerFactory.getLogger(Project3.class);

    private static final String PROPERTIES_FILENAME = "project3.properties";
    public static final String DEFAULT_MESSAGE_COUNT = "1000";

    public static void main(String[] args) throws InterruptedException {
        Properties properties;
        try {
            properties = new PropertiesLoader(PROPERTIES_FILENAME).loadProperties();
        } catch (Exception e) {
            logger.error("Failed to load properties from " + PROPERTIES_FILENAME, e);
            return;
        }

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(properties.getProperty("url"));
        Connection connection;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
        } catch (JMSException e) {
            logger.error("Failed to create JMS connection.", e);
            return;
        }

        String count = System.getProperty("count", DEFAULT_MESSAGE_COUNT);

        try {
            connection.close();
        } catch (JMSException e) {
            logger.error("Failed to close JMS connection.", e);
        }
    }
}
