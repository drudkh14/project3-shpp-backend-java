package com.zhbohdanchykov;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final Logger PRINTER = LoggerFactory.getLogger("PrinterLogger");

    private static final int THREADS_NUMBER = 10;

    private static final String VALID_FILENAME = "valid_messages.csv";
    private static final String INVALID_FILENAME = "invalid_messages.csv";
    private static final String[] VALID_HEADER = {"name", "count"};
    private static final String[] INVALID_HEADER = {"name", "count", "errors"};

    private static final BlockingQueue<MessagePOJO> VALID_QUEUE = new LinkedBlockingQueue<>();
    private static final BlockingQueue<MessagePOJO> INVALID_QUEUE = new LinkedBlockingQueue<>();

    private final ProjectProperties properties;

    public Application(ProjectProperties properties) {
        this.properties = properties;
    }

    public void start() {
        ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(properties.getUrl());
        factory.setTrustedPackages(List.of("com.zhbohdanchykov.MessagePOJO"));

        try (
                Connection connection = factory.createConnection()) {
            connection.start();

            ExecutorServiceManager manager = new ExecutorServiceManager(THREADS_NUMBER);

            ArrayList<Producer> producers = prepareProducers(connection, MessageGenerator::generate);
            ArrayList<Consumer> consumers = prepareConsumers(connection, new MessageRouter(VALID_QUEUE, INVALID_QUEUE));
            ArrayList<WriterCSV> writers = prepareWriters();

            long startTime = System.currentTimeMillis();

            manager.launch(producers, consumers, writers);
            manager.terminate();
            ProcessingResults results = manager.getResults();

            long endTime = System.currentTimeMillis();
            float elapsedTime = (float) (endTime - startTime) / 1000;
            LOGGER.info("Time: {} s", elapsedTime);
            PRINTER.info("Time: {} s", elapsedTime);
            LOGGER.info("Total messages sent: {}", results.messagesSent());
            PRINTER.info("Total messages sent: {}", results.messagesSent());
            LOGGER.info("Total messages received: {}", results.messagesReceived());
            PRINTER.info("Total messages received: {}", results.messagesReceived());
            LOGGER.info("Total messages written: {}", results.messagesWritten());
            PRINTER.info("Total messages written: {}", results.messagesWritten());
            LOGGER.info("Messages per second: {}", results.messagesWritten() / elapsedTime);
            PRINTER.info("Messages per second: {}", results.messagesWritten() / elapsedTime);
        } catch (JMSException e) {
            LOGGER.error("Failed to create a JMS connection.");
        }
    }

    private ArrayList<Producer> prepareProducers(Connection connection, Supplier<MessagePOJO> messageGenerator) {
        ArrayList<Producer> res = new ArrayList<>();

        int messagesNumber = properties.getMessagesNumber();
        String queueName = properties.getQueueName();
        int batch = messagesNumber / THREADS_NUMBER;
        int rest = messagesNumber % THREADS_NUMBER;

        for (int i = 0; i < THREADS_NUMBER; i++) {
            if (i == THREADS_NUMBER - 1) {
                res.add(new Producer(connection, queueName, batch + rest,
                        properties.getStopTime(), MessageGenerator::generate));
            } else {
                res.add(new Producer(connection, queueName, batch, properties.getStopTime(),
                        messageGenerator));
            }
        }

        return res;
    }

    private ArrayList<Consumer> prepareConsumers(Connection connection, MessageRouter router) {
        ArrayList<Consumer> res = new ArrayList<>();

        for (int i = 0; i < THREADS_NUMBER; i++) {
            res.add(new Consumer(connection, properties.getQueueName(), router));
        }

        return res;
    }

    private ArrayList<WriterCSV> prepareWriters() {
        ArrayList<WriterCSV> res = new ArrayList<>();

        res.add(new WriterCSV(VALID_QUEUE, VALID_FILENAME, VALID_HEADER, THREADS_NUMBER));
        res.add(new WriterCSV(INVALID_QUEUE, INVALID_FILENAME, INVALID_HEADER, THREADS_NUMBER));

        return res;
    }
}
