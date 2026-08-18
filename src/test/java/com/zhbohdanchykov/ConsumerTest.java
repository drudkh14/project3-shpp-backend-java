package com.zhbohdanchykov;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.time.LocalDateTime;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.*;

class ConsumerTest {

    private final static ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    public static final String TEST_QUEUE_NAME = "test";
    public static final String TEST_EDDR = "19910824-00026";
    public static final int TEST_COUNT = 1;

    private Consumer consumer;

    private MessageConsumer messageConsumer;
    private TextMessage textMessage;
    private TextMessage poisonPill;
    private final MessagePOJO messagePOJO = new MessagePOJO(TEST_QUEUE_NAME,
            TEST_EDDR, TEST_COUNT, LocalDateTime.now());
    private MessageRouter router;

    @BeforeEach
    void setUp() throws JMSException, JsonProcessingException {
        Connection connection = mock(Connection.class);
        messageConsumer = mock(MessageConsumer.class);
        Session session = mock(Session.class);
        textMessage = mock(TextMessage.class);
        poisonPill = mock(TextMessage.class);
        Queue queue = mock(Queue.class);
        router = mock(MessageRouter.class);

        when(connection.createSession(false, Session.AUTO_ACKNOWLEDGE)).thenReturn(session);
        when(session.createQueue("test")).thenReturn(queue);
        when(session.createConsumer(queue)).thenReturn(messageConsumer);
        when(session.createTextMessage()).thenReturn(textMessage);
        when(textMessage.getText()).thenReturn(MAPPER.writeValueAsString(messagePOJO));
        when(poisonPill.getText()).thenReturn(MAPPER.writeValueAsString(MessagePOJO.poison()));

        consumer = new Consumer(connection, "test", router);
    }

    @Test
    void shouldProcessRegularMessage() throws JMSException, InterruptedException {
        when(messageConsumer.receive()).thenReturn(textMessage).thenReturn(poisonPill);

        int result = consumer.call();

        assertEquals(1, result);
        verify(router, times(2)).routeMessage(any(MessagePOJO.class));
    }

    @Test
    void shouldStopWhenPoisonPullReceived() throws JMSException, InterruptedException {
        when(messageConsumer.receive()).thenReturn(textMessage).thenReturn(poisonPill).thenReturn(textMessage);

        int result = consumer.call();

        assertEquals(1, result);

        verify(router, times(2)).routeMessage(any(MessagePOJO.class));

        verify(textMessage).getText();
        verify(poisonPill).getText();
    }

    @Test
    void shouldCountAllRegularMessagesBeforePoisonPill() throws JMSException, InterruptedException {
        when(messageConsumer.receive()).
                thenReturn(textMessage).thenReturn(textMessage).thenReturn(textMessage).thenReturn(textMessage).
                thenReturn(textMessage).thenReturn(poisonPill);

        int result = consumer.call();

        assertEquals(5, result);

        verify(router, times(6)).routeMessage(any(MessagePOJO.class));
    }
}