package com.zhbohdanchykov;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLOutput;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

public class MessagePOJOProducer implements Runnable {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Logger logger = LoggerFactory.getLogger(MessagePOJOProducer.class);
    private final Connection connection;
    private final String queue;
    private final int count;
    private final int stop;

    public MessagePOJOProducer(Connection connection, String queue, int count, int stop) {
        this.connection = connection;
        this.queue = queue;
        this.count = count;
        this.stop = stop;
    }

    @Override
    public void run() {
        Instant now = Instant.now();
        Instant stopTime = now.plusSeconds(stop);

        try (
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageProducer producer = session.createProducer(session.createQueue(queue));
        ) {
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);

            Stream.generate(MessageGenerator::generateMessage)
                    .limit(count)
                    .takeWhile(msg -> Instant.now().isBefore(stopTime))
                    .forEach(msg -> {
                        String json;
                        try {
                            json = mapper.writeValueAsString(msg);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println(json);
                        TextMessage textMessage;
                        try {
                            textMessage = session.createTextMessage(json);
                        } catch (JMSException e) {
                            throw new RuntimeException(e);
                        }
                        try {
                            producer.send(textMessage);
                        } catch (JMSException e) {
                            throw new RuntimeException(e);
                        }
                    });
            producer.send(session.createTextMessage("STOP"));
        } catch (JMSException e) {
            logger.error("Failed to start producer.", e);
        }
    }
}
