package com.zhbohdanchykov;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class MessagePOJOProducer implements Runnable {

    private static final ObjectMapper mapper = new ObjectMapper();
    private ConnectionFactory connectionFactory;
    private String queue;
    private int count;
    private int stop;

    public MessagePOJOProducer(ConnectionFactory connectionFactory, String queue, int count, int stop) {
        this.connectionFactory = connectionFactory;
        this.queue = queue;
        this.count = count;
        this.stop = stop;
    }

    @Override
    public void run() {
        mapper.registerModule(new JavaTimeModule());
        Instant now = Instant.now();
        Instant stopTime = now.plusSeconds(stop);
        AtomicInteger counter = new AtomicInteger();


        Connection connection;
        Session session;
        MessageProducer producer;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(queue);
            producer = session.createProducer(destination);
            producer.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        MessageProducer finalProducer = producer;
        Session finalSession = session;
        Stream.generate(MessageGenerator::generateMessage)
                .limit(count)
                .takeWhile(obj -> Instant.now().isBefore(stopTime))
                .peek(obj -> counter.getAndIncrement())
                .forEach(obj -> {
                    try {
                        finalProducer.send(finalSession.createTextMessage(mapper.writeValueAsString(obj)));
                    } catch (JMSException | JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
        for (int i = 0; i < 10; i++) {
            try {
                producer.send(session.createTextMessage("STOP"));
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Sent " + count + " messages to " + queue);
        System.out.println(Duration.between(now, Instant.now()).toMillis() + " ms");
        try {
            producer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
