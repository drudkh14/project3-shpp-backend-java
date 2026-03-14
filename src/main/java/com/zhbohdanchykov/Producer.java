package com.zhbohdanchykov;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Producer implements Callable<Integer> {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Logger LOGGER = LoggerFactory.getLogger(Producer.class);
    private final Connection connection;
    private final String queue;
    private final int count;
    private final int stop;

    public Producer(Connection connection, String queue, int count, int stop) {
        this.connection = connection;
        this.queue = queue;
        this.count = count;
        this.stop = stop;
    }

    @Override
    public Integer call() {
        Instant now = Instant.now();
        Instant stopTime = now.plusSeconds(stop);
        AtomicInteger messageCount = new AtomicInteger();

        try (
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer producer = session.createProducer(session.createQueue(queue))
        ) {
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.setDisableMessageID(true);
            producer.setDisableMessageTimestamp(true);

            TextMessage textMessage = session.createTextMessage();
            Stream.generate(MessageGenerator::generateMessage)
                    .limit(count)
                    .takeWhile(msg -> Instant.now().isBefore(stopTime))
                    .forEach(msg -> {
                        try {
                            textMessage.setText(MAPPER.writeValueAsString(msg));
                            LOGGER.debug("Sending message {}", msg);
                            producer.send(textMessage);
                            LOGGER.debug("Sent message {}", msg);

                            messageCount.getAndIncrement();
                        } catch (JMSException e) {
                            LOGGER.error("Failed to send message.", e);
                        } catch (JsonProcessingException e) {
                            LOGGER.error("Failed to serialize message.", e);
                        }
                    });
            LOGGER.info("Sending poison pill.");
            producer.send(session.createTextMessage("STOP"));
            LOGGER.info("Poison pill sent.");
        } catch (JMSException e) {
            LOGGER.error("Failed to start producer.", e);
        }
        return messageCount.get();
    }
}
