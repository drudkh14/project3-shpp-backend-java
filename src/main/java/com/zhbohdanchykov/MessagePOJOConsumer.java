package com.zhbohdanchykov;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MessagePOJOConsumer implements Runnable {

    private ObjectMapper mapper = new ObjectMapper();
    private ConnectionFactory connectionFactory;
    private String queue;

    public MessagePOJOConsumer(ConnectionFactory connectionFactory, String queue) {
        this.connectionFactory = connectionFactory;
        this.queue = queue;
    }

//    public void consume() throws Exception {
//        mapper.registerModule(new JavaTimeModule());
//
//        Connection connection = connectionFactory.createConnection();
//        connection.start();
//        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//        Destination destination = session.createQueue(queue);
//        MessageConsumer consumer = session.createConsumer(destination);
//        int counter = 0;
//
//        while (true) {
//            Message msg = consumer.receive();
//            if (msg == null) {
//                break;
//            }
//            TextMessage textMessage = (TextMessage) msg;
//            String text = textMessage.getText();
//            if (!text.equals("STOP")) {
//                MessagePOJO messagePOJO = mapper.readValue(text, MessagePOJO.class);
//                ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//                Validator validator = factory.getValidator();
//                Set<ConstraintViolation<MessagePOJO>> violations = validator.validate(messagePOJO);
//                writeMessage(messagePOJO, violations);
//                counter++;
//            } else {
//                break;
//            }
//        }
//        System.out.println("Consumed " + counter + " messages");
//        consumer.close();
//        session.close();
//        connection.close();
//    }

    private void writeMessage(MessagePOJO messagePOJO, Set<ConstraintViolation<MessagePOJO>> violations)
            throws IOException {
        File file = violations.isEmpty() ? new File("valid_messages.csv") :
                new File("invalid_messages.csv");
        String[] header = violations.isEmpty() ?
                new String[]{"name", "count"} :
                new String[]{"name", "count", "errors"};
        CSVFormat format = file.exists() ? CSVFormat.DEFAULT : CSVFormat.DEFAULT.withHeader(header);
        FileWriter out = new FileWriter(file, true);
        List<String> errorsList = violations.stream().map(ConstraintViolation::getMessage).toList();
        Map<String, List<String>> errors = new HashMap<>();
        errors.put("errors", errorsList);
        try (CSVPrinter printer = new CSVPrinter(out, format)) {
            if (!violations.isEmpty()) {
                printer.printRecord(messagePOJO.getName(), messagePOJO.getCount(), mapper.writeValueAsString(errors));
            } else {
                printer.printRecord(messagePOJO.getName(), messagePOJO.getCount());
            }
        }
    }

    @Override
    public void run() {
        mapper.registerModule(new JavaTimeModule());

        Connection connection = null;
        Session session = null;
        MessageConsumer consumer = null;
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Destination destination = session.createQueue(queue);
            consumer = session.createConsumer(destination);
        } catch (JMSException e) {
            e.printStackTrace();
        }

        int counter = 0;

        while (true) {
            Message msg;
            try {
                msg = consumer.receive();
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
            if (msg == null) {
                break;
            }
            TextMessage textMessage = (TextMessage) msg;
            String text;
            try {
                text = textMessage.getText();
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
            if (!text.equals("STOP")) {
                MessagePOJO messagePOJO;
                try {
                    messagePOJO = mapper.readValue(text, MessagePOJO.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
                Validator validator = factory.getValidator();
                Set<ConstraintViolation<MessagePOJO>> violations = validator.validate(messagePOJO);
                try {
                    writeMessage(messagePOJO, violations);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                counter++;
            } else {
                break;
            }
        }
        System.out.println("Consumed " + counter + " messages");
        try {
            consumer.close();
            session.close();
            connection.close();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }
    }
}
