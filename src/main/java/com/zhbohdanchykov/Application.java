package com.zhbohdanchykov;

import jakarta.jms.Connection;
import jakarta.jms.JMSException;
import jakarta.validation.Validator;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Supplier;

public class Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);
    private static final Logger PRINTER = LoggerFactory.getLogger("PrinterLogger");

    private static final int THREADS_NUMBER = 10;
    private static final int THREAD_NUMBERS_WRITERS = 2;

    private static final String VALID_FILENAME = "valid_messages.csv";
    private static final String INVALID_FILENAME = "invalid_messages.csv";
    private static final String[] VALID_HEADER = {"name", "count"};
    private static final String[] INVALID_HEADER = {"name", "count", "errors"};

    private static final BlockingQueue<MessagePOJO> VALID_QUEUE = new LinkedBlockingQueue<>();
    private static final BlockingQueue<MessagePOJO> INVALID_QUEUE = new LinkedBlockingQueue<>();


    private final ProjectProperties properties;
    private final Validator validator;

    public Application(ProjectProperties properties, Validator validator) {
        this.properties = properties;
        this.validator = validator;
    }

    public void start() {
        ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory(properties.getUrl());
        activeMQConnectionFactory.setTrustedPackages(List.of("com.zhbohdanchykov.MessagePOJO"));

        try (
                Connection connection = activeMQConnectionFactory.createConnection()
        ) {
            connection.start();

            List<ExecutorServiceManagerEntry<Integer>> entries = prepareEntries(connection,
                    MessageGenerator::generate,
                    new MessageRouter(VALID_QUEUE, INVALID_QUEUE, validator)
            );

            ExecutorServiceManager<Integer> manager = new ExecutorServiceManager<>(entries);

            long startTime = System.currentTimeMillis();

            manager.launch();
            manager.terminate();

            ProcessingResults results = getResults(entries);

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

    private List<ExecutorServiceManagerEntry<Integer>> prepareEntries(
            Connection connection, Supplier<MessagePOJO> generator, MessageRouter router
    ) {
        List<ExecutorServiceManagerEntry<Integer>> res = new ArrayList<>();

        ExecutorServiceManagerEntry<Integer> producersEntry = new ExecutorServiceManagerEntry<>(
                Executors.newFixedThreadPool(THREADS_NUMBER), prepareProducers(connection, generator), new ArrayList<>()
        );
        ExecutorServiceManagerEntry<Integer> consumersEntry = new ExecutorServiceManagerEntry<>(
                Executors.newFixedThreadPool(THREADS_NUMBER), prepareConsumers(connection, router), new ArrayList<>()
        );
        ExecutorServiceManagerEntry<Integer> writersEntry = new ExecutorServiceManagerEntry<>(
                Executors.newFixedThreadPool(THREAD_NUMBERS_WRITERS), prepareWriters(), new ArrayList<>()
        );

        res.add(producersEntry);
        res.add(consumersEntry);
        res.add(writersEntry);

        return res;
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

    private ProcessingResults getResults(List<ExecutorServiceManagerEntry<Integer>> entries) {
        int totalMessagesSent = countResult(entries.get(0).results());
        int totalMessagesReceived = countResult(entries.get(1).results());
        int totalMessagesWritten = countResult(entries.get(1).results());

        return new ProcessingResults(totalMessagesSent, totalMessagesReceived, totalMessagesWritten);
    }

    private int countResult(List<Future<Integer>> results) {
        int res = 0;

        for (Future<Integer> future : results) {
            try {
                res += future.get();
            } catch (InterruptedException e) {
                LOGGER.error("Failed getting result from {} because of interruption.", results);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOGGER.error("Task from {} was failed.", results, e.getCause());
            }
        }

        return res;
    }

}
