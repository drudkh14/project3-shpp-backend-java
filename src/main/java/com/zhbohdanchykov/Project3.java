package com.zhbohdanchykov;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Project3 {
    public static final int THREADS_NUMBER = 10;
    private static final Logger logger = LoggerFactory.getLogger(Project3.class);

    private static final String PROPERTIES_FILENAME = "project3.properties";
    public static final String DEFAULT_MESSAGE_COUNT = "10000";

    public static void main(String[] args) {
        Properties properties;
        try {
            properties = new PropertiesLoader(PROPERTIES_FILENAME).loadProperties();
        } catch (Exception e) {
            logger.error("Failed to load properties from " + PROPERTIES_FILENAME, e);
            return;
        }

        try (
                Connection connection = new ActiveMQConnectionFactory(properties.getProperty("url")).createConnection();
        ) {
            connection.start();
            ExecutorService producerPool = Executors.newFixedThreadPool(THREADS_NUMBER);
            ExecutorService consumerPool = Executors.newFixedThreadPool(THREADS_NUMBER);

            for (int i = 0; i < THREADS_NUMBER; i++) {
                producerPool.submit(new MessagePOJOProducer(connection, properties.getProperty("queue"),
                        Integer.parseInt(System.getProperty("count", DEFAULT_MESSAGE_COUNT)) / THREADS_NUMBER,
                        Integer.parseInt(properties.getProperty("stop"))));
            }

            for (int i = 0; i < THREADS_NUMBER; i++) {
                consumerPool.submit(new MessagePOJOConsumer(connection, properties.getProperty("queue")));
            }

            producerPool.shutdown();
            consumerPool.shutdown();
            producerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            consumerPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (JMSException e) {
            logger.error("Failed to create a JMS connection.", e);
        } catch (InterruptedException e) {
            logger.error("Failed to close ExecutorService.", e);
        }
    }
}
