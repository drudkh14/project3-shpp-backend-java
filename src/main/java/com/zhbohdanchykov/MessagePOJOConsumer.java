package com.zhbohdanchykov;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;


public class MessagePOJOConsumer implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MessagePOJOConsumer.class);
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Connection connection;
    private final String queue;
    private final BlockingQueue<MessagePOJO> validQueue;
    private final BlockingQueue<MessagePOJO> invalidQueue;

    public MessagePOJOConsumer(Connection connection, String queue,
                               BlockingQueue<MessagePOJO> validQueue, BlockingQueue<MessagePOJO> invalidQueue) {
        this.connection = connection;
        this.queue = queue;
        this.validQueue = validQueue;
        this.invalidQueue = invalidQueue;
    }

    @Override
    public Integer call() {
        AtomicInteger messageCount = new AtomicInteger(0);
        try (
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageConsumer consumer = session.createConsumer(session.createQueue(queue))
        ) {
            boolean reading = true;

            while (reading) {
                Message message = consumer.receive();
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                LOGGER.debug("Message received: {}", text);
                if (!text.equals("STOP")) {
                    MessagePOJO messagePOJO = MAPPER.readValue(text, MessagePOJO.class);
                    Set<ConstraintViolation<MessagePOJO>> violations = VALIDATOR.validate(messagePOJO);
                    if (violations.isEmpty()) {
                        validQueue.put(messagePOJO);
                    } else {
                        messagePOJO.setErrors(violations.stream().map(ConstraintViolation::getMessage).toList());
                        invalidQueue.put(messagePOJO);
                    }
                    messageCount.incrementAndGet();
                } else {
                    LOGGER.info("Received poison pill");
                    reading = false;
                    LOGGER.info("Total messages received: {}", messageCount);
                    validQueue.put(MessagePOJO.poison());
                    invalidQueue.put(MessagePOJO.poison());
                }
            }
        } catch (JMSException e) {
            LOGGER.error("Failed to start consumer.", e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse JSON message.", e);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to put a message in a BlockingQueue.", e);
            Thread.currentThread().interrupt();
        }

        return messageCount.get();
    }
}
