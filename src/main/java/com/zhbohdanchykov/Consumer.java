package com.zhbohdanchykov;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class Consumer implements Callable<Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Consumer.class);
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final Connection connection;
    private final String queue;
    private final MessageRouter router;

    public Consumer(Connection connection, String queue,
                    MessageRouter router) {
        this.connection = connection;
        this.queue = queue;
        this.router = router;
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
                TextMessage textMessage = (TextMessage) consumer.receive();
                String text = textMessage.getText();
                LOGGER.debug("Message received: {}", text);
                if (!text.equals("STOP")) {
                    MessagePOJO messagePOJO = MAPPER.readValue(text, MessagePOJO.class);
                    router.routeMessage(messagePOJO);
                    messageCount.incrementAndGet();
                } else {
                    LOGGER.info("Received poison pill");
                    reading = false;
                    LOGGER.info("Messages from a consumer received: {}", messageCount);
                    router.routeMessage(MessagePOJO.poison());
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
