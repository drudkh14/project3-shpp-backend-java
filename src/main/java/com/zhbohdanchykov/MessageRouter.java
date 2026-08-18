package com.zhbohdanchykov;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class MessageRouter {

    private final Validator validator;

    private final BlockingQueue<MessagePOJO> validQueue;
    private final BlockingQueue<MessagePOJO> invalidQueue;

    public MessageRouter(BlockingQueue<MessagePOJO> validQueue, BlockingQueue<MessagePOJO> invalidQueue,
                         Validator validator) {
        this.validQueue = validQueue;
        this.invalidQueue = invalidQueue;
        this.validator = validator;
    }

    public void routeMessage(MessagePOJO message) throws InterruptedException {
        if (message.getIsPoisonPill()) {
            validQueue.put(message);
            invalidQueue.put(message);
        } else {
            Set<ConstraintViolation<MessagePOJO>> violations = validator.validate(message);
            if (violations.isEmpty()) {
                validQueue.put(message);
            } else {
                message.setErrors(violations.stream().map(ConstraintViolation::getMessage).toList());
                invalidQueue.put(message);
            }
        }
    }
}
