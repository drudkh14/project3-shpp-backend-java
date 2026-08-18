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
import java.util.function.Supplier;
import java.util.stream.Stream;

public class Producer implements Callable<Integer> {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Logger LOGGER = LoggerFactory.getLogger(Producer.class);

    private final Connection connection;
    private final String queue;
    private final int count;
    private final int stop;
    private final Supplier<MessagePOJO> messageGenerator;

    public Producer(Connection connection, String queue, int count, int stop, Supplier<MessagePOJO> messageGenerator) {
        this.connection = connection;
        this.queue = queue;
        this.count = count;
        this.stop = stop;
        this.messageGenerator = messageGenerator;
    }

    @Override
    public Integer call() {
        AtomicInteger messageCount = new AtomicInteger();
        Instant now = Instant.now();
        Instant stopTime = now.plusSeconds(stop);

        try (
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer producer = session.createProducer(session.createQueue(queue))
        ) {
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            producer.setDisableMessageID(true);
            producer.setDisableMessageTimestamp(true);

            TextMessage textMessage = session.createTextMessage();
            Stream.generate(messageGenerator)
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
                            LOGGER.error("Failed to serialize message: {}.", msg, e);
                        }
                    });
            LOGGER.info("Sending poison pill.");
            textMessage.setText(MAPPER.writeValueAsString(MessagePOJO.poison()));
            producer.send(textMessage);
            LOGGER.info("Poison pill sent.");
        } catch (JMSException e) {
            LOGGER.error("Failed to start producer.", e);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to serialize message: {}", MessagePOJO.poison(), e);
        }
        return messageCount.get();
    }
}
