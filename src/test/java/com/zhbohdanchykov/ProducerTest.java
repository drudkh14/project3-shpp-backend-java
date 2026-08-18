package com.zhbohdanchykov;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.*;

class ProducerTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    public static final String TEST_QUEUE_NAME = "test";

    private Connection connection;
    private MessageProducer messageProducer;
    private TextMessage textMessage;

    @BeforeEach
    void setUp() throws JMSException {
        connection = mock(Connection.class);
        Session session = mock(Session.class);
        messageProducer = mock(MessageProducer.class);
        textMessage = mock(TextMessage.class);
        Queue queue = mock(Queue.class);

        when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);
        when(session.createQueue("test")).thenReturn(queue);
        when(session.createProducer(queue)).thenReturn(messageProducer);
        when(session.createTextMessage()).thenReturn(textMessage);

    }

    @Test
    void shouldSendRequestedNumberOfMessages() throws JMSException {
        Producer producer = new Producer(connection, TEST_QUEUE_NAME, 10, 60, MessagePOJO::new);

        producer.call();

        verify(messageProducer, times(11)).send(any(TextMessage.class));
    }

    @Test
    void shouldReturnNumberOfSentMessages() {
        Producer producer = new Producer(connection, "test", 10, 60, MessagePOJO::new);

        int result = producer.call();

        assertEquals(10, result);
    }

    @Test
    void shouldSendPoisonPill() throws JMSException, JsonProcessingException {
        MessagePOJO messagePOJO = new MessagePOJO("test", "19910824-00026", 1, LocalDateTime.now());

        Producer producer = new Producer(connection, "test", 1, 60,
                () -> messagePOJO);

        producer.call();

        InOrder inOrder = inOrder(textMessage, messageProducer);

        inOrder.verify(textMessage).setText(MAPPER.writeValueAsString(messagePOJO));
        inOrder.verify(messageProducer).send(textMessage);

        inOrder.verify(textMessage).setText(MAPPER.writeValueAsString(MessagePOJO.poison()));
        inOrder.verify(messageProducer).send(textMessage);
    }

    @Test
    void shouldStopSendingWhenTimeIsReached() {
        Producer producer = new Producer(connection, "test", 1000000, 1,
                MessagePOJO::new);

        long start = System.nanoTime();

        producer.call();

        long end = System.nanoTime();

        assertTrue(TimeUnit.NANOSECONDS.toSeconds(end - start) >= 1);
    }

    @Test
    void shouldRequestRequestedNumberOfMessagesFromGenerator() {
        @SuppressWarnings("unchecked")
        Supplier<MessagePOJO> supplier = mock(Supplier.class);
        when(supplier.get()).thenReturn(new MessagePOJO());

        Producer producer = new Producer(connection, "test", 10, 60,
                supplier);

        producer.call();

        verify(supplier, times(10)).get();
    }
}