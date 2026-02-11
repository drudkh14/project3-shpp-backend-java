package com.zhbohdanchykov;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.jms.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MessagePOJOConsumer implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(MessagePOJOConsumer.class);
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final Connection connection;
    private final String queue;

    public MessagePOJOConsumer(Connection connection, String queue) {
        this.connection = connection;
        this.queue = queue;
    }

    @Override
    public void run() {
        try (
                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
                MessageConsumer consumer = session.createConsumer(session.createQueue(queue))
        ) {
            boolean reading = true;
            while (reading) {
                Message message = consumer.receive();
                TextMessage textMessage = (TextMessage) message;
                String text = textMessage.getText();
                if (!text.equals("STOP")) {
                    MessagePOJO messagePOJO = mapper.readValue(text, MessagePOJO.class);
                    Set<ConstraintViolation<MessagePOJO>> violations = validator.validate(messagePOJO);
                    writeMessageToCSV(messagePOJO, violations);
                } else {
                    reading = false;
                }
            }
        } catch (JMSException e) {
            logger.error("Failed to start consumer.", e);
        } catch (IOException e) {
            logger.error("Failed to write message to CSV.", e);
        }
    }

    private void writeMessageToCSV(MessagePOJO messagePOJO, Set<ConstraintViolation<MessagePOJO>> violations)
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
}
