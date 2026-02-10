package com.zhbohdanchykov;

import jakarta.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class Project3 {
    public static final int THREADS_NUMBER = 10;
    private static final Logger logger = LoggerFactory.getLogger(Project3.class);

    private static final String PROPERTIES_FILENAME = "project3.properties";
    public static final String DEFAULT_MESSAGE_COUNT = "1000";

    public static void main(String[] args) {
        Properties properties;
        try {
            properties = new PropertiesLoader(PROPERTIES_FILENAME).loadProperties();
        } catch (Exception e) {
            logger.error(String.valueOf(e));
            return;
        }

        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(properties.getProperty("url"));

        String count = System.getProperty("count", DEFAULT_MESSAGE_COUNT);

        MessagePOJOProducer producer = new MessagePOJOProducer(connectionFactory, properties.getProperty("queue"),
                Integer.parseInt(count) / THREADS_NUMBER,
                Integer.parseInt(properties.getProperty("stop")));

        for (int i = 0; i < THREADS_NUMBER; i++) {
            Thread thread = new Thread(producer);
            thread.start();
        }

        MessagePOJOConsumer consumer = new MessagePOJOConsumer(connectionFactory, properties.getProperty("queue"));

        for (int i = 0; i < THREADS_NUMBER; i++) {
            Thread thread = new Thread(consumer);
            thread.start();
        }
    }
}
