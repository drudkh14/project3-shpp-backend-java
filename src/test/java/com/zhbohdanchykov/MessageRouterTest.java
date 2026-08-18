package com.zhbohdanchykov;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.concurrent.BlockingQueue;

import static junit.framework.Assert.assertFalse;
import static org.mockito.Mockito.*;

class MessageRouterTest {

    private MessageRouter router;
    private BlockingQueue<MessagePOJO> validQueue;
    private BlockingQueue<MessagePOJO> invalidQueue;
    private static Validator validator;

    @BeforeAll
    static void setUpAll() {
        try(ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        validQueue = mock(BlockingQueue.class);
        invalidQueue = mock(BlockingQueue.class);
        router = new MessageRouter(validQueue, invalidQueue, validator);
    }

    @Test
    void shouldGoToValidQueue() throws InterruptedException {
        MessagePOJO messagePOJO = new MessagePOJO("Andreas", "19910824-00026",
                11, LocalDateTime.now());

        router.routeMessage(messagePOJO);

        verify(validQueue).put(messagePOJO);
        verify(invalidQueue, never()).put(any());
    }

    @Test
    void shouldGoToInvalidQueue() throws InterruptedException {
        MessagePOJO messagePOJO = new MessagePOJO("test", "19910824-00025",
                7, LocalDateTime.now());

        router.routeMessage(messagePOJO);

        verify(invalidQueue).put(messagePOJO);
        verify(validQueue, never()).put(any());
        assertFalse(messagePOJO.getErrors().isEmpty());
    }

    @Test
    void shouldGoToBothPoisonPill() throws InterruptedException {
        MessagePOJO poisonPill = MessagePOJO.poison();

        router.routeMessage(poisonPill);

        verify(validQueue).put(poisonPill);
        verify(invalidQueue).put(poisonPill);
    }
}