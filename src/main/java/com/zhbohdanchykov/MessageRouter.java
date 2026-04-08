package com.zhbohdanchykov;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class MessageRouter {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final BlockingQueue<MessagePOJO> validQueue;
    private final BlockingQueue<MessagePOJO> invalidQueue;

    public MessageRouter(BlockingQueue<MessagePOJO> validQueue, BlockingQueue<MessagePOJO> invalidQueue) {
        this.validQueue = validQueue;
        this.invalidQueue = invalidQueue;
    }

    public void routeMessage(MessagePOJO message) throws InterruptedException {
        if (message.getIsPoisonPill()) {
            validQueue.put(message);
            invalidQueue.put(message);
        } else {
            Set<ConstraintViolation<MessagePOJO>> violations = VALIDATOR.validate(message);
            if (violations.isEmpty()) {
                validQueue.put(message);
            } else {
                message.setErrors(violations.stream().map(ConstraintViolation::getMessage).toList());
                invalidQueue.put(message);
            }
        }
    }
}
