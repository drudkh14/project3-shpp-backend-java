package com.zhbohdanchykov;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class Project3 {

    private static final Logger LOGGER = LoggerFactory.getLogger(Project3.class);
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private static final int THREADS_NUMBER = 10;

    private static final String PROPERTIES_FILENAME = "project3.properties";
    private static final String DEFAULT_MESSAGE_COUNT = "10000";

    private static final String VALID_FILENAME = "valid_messages.csv";
    private static final String INVALID_FILENAME = "invalid_messages.csv";
    private static final String[] VALID_HEADER = {"name", "count"};
    private static final String[] INVALID_HEADER = {"name", "count", "errors"};

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

        int count = Integer.parseInt(System.getProperty("count", DEFAULT_MESSAGE_COUNT));
        int stop = properties.getStopTime();
        String queue = properties.getQueueName();
        String url = properties.getUrl();

        BlockingQueue<MessagePOJO> validQueue = new LinkedBlockingQueue<>();
        BlockingQueue<MessagePOJO> invalidQueue = new LinkedBlockingQueue<>();

        MessageWriterCSV validWriter = new MessageWriterCSV(validQueue, VALID_FILENAME, VALID_HEADER, THREADS_NUMBER);
        MessageWriterCSV invalidWriter = new MessageWriterCSV(invalidQueue, INVALID_FILENAME, INVALID_HEADER, THREADS_NUMBER);

        try (
                Connection connection = new ActiveMQConnectionFactory(url).createConnection();
                ExecutorService producerPool = Executors.newFixedThreadPool(THREADS_NUMBER);
                ExecutorService consumerPool = Executors.newFixedThreadPool(THREADS_NUMBER);
                ExecutorService writerPool = Executors.newFixedThreadPool(2)
        ) {
            connection.start();

            long startTime = System.currentTimeMillis();

            List<Future<Integer>> producerFutures = new ArrayList<>();
            List<Future<Integer>> consumerFutures = new ArrayList<>();
            List<Future<Integer>> writerFutures = new ArrayList<>();

            for (int i = 0; i < THREADS_NUMBER; i++) {
                producerFutures.add(
                        producerPool.submit(
                                new MessagePOJOProducer(connection, queue, count / THREADS_NUMBER, stop))
                );
            }

            for (int i = 0; i < THREADS_NUMBER; i++) {
                consumerFutures.add(
                        consumerPool.submit(new MessagePOJOConsumer(connection, queue, validQueue, invalidQueue)));
            }

            writerFutures.add(writerPool.submit(validWriter));
            writerFutures.add(writerPool.submit(invalidWriter));

            producerPool.shutdown();
            consumerPool.shutdown();
            writerPool.shutdown();

            try {
                if (!producerPool.awaitTermination(600, TimeUnit.SECONDS)) {
                    producerPool.shutdownNow();
                    if (!producerPool.awaitTermination(600, TimeUnit.SECONDS)) {
                        LOGGER.error("Producer pool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                producerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }

            try {
                if (!consumerPool.awaitTermination(600, TimeUnit.SECONDS)) {
                    consumerPool.shutdownNow();
                    if (!consumerPool.awaitTermination(600, TimeUnit.SECONDS)) {
                        LOGGER.error("Consumer pool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                consumerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }

            try {
                if (!writerPool.awaitTermination(600, TimeUnit.SECONDS)) {
                    writerPool.shutdownNow();
                    if (!writerPool.awaitTermination(600, TimeUnit.SECONDS)) {
                        LOGGER.error("Writer pool did not terminate");
                    }
                }
            } catch (InterruptedException e) {
                writerPool.shutdownNow();
                Thread.currentThread().interrupt();
            }

            int totalMessagesSent = 0;
            int totalMessagesReceived = 0;
            int totalMessagesWritten = 0;

            for (Future<Integer> future : producerFutures) {
                try {
                    totalMessagesSent += future.get();
                } catch (ExecutionException e) {
                    LOGGER.error("Producer task was failed.", e.getCause());
                }
            }

            for (Future<Integer> future : consumerFutures) {
                try {
                    totalMessagesReceived += future.get();
                } catch (ExecutionException e) {
                    LOGGER.error("Consumer task was failed.", e.getCause());
                }
            }

            for (Future<Integer> future : writerFutures) {
                try {
                    totalMessagesWritten += future.get();
                } catch (ExecutionException e) {
                    LOGGER.error("Writer task was failed.", e.getCause());
                }
            }

            long endTime = System.currentTimeMillis();
            float elapsedTime = (float) (endTime - startTime) / 1000;
            LOGGER.info("Time: {}", elapsedTime + " s");
            LOGGER.info("Total messages sent: {}", totalMessagesSent);
            LOGGER.info("Total messages received: {}", totalMessagesReceived);
            LOGGER.info("Total messages written: {}", totalMessagesWritten);
            LOGGER.info("Messages per second: {}", totalMessagesWritten / elapsedTime);
        } catch (JMSException e) {
            LOGGER.error("Failed to create a JMS connection.", e);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to put poison pill to queue.", e);
        }
    }
}
