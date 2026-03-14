package com.zhbohdanchykov;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

public class WriterCSV implements Callable<Integer> {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());
    private static final Logger LOGGER = LoggerFactory.getLogger(WriterCSV.class);

    private final BlockingQueue<MessagePOJO> queue;
    private final String filename;
    private final String[] header;
    private final int poisonPillCount;

    public WriterCSV(BlockingQueue<MessagePOJO> queue, String filename, String[] header,
                     int poisonPillCount) {
        this.queue = queue;
        this.filename = filename;
        this.header = header;
        this.poisonPillCount = poisonPillCount;
    }

    @Override
    public Integer call() {
        boolean writing = true;
        AtomicInteger messageCount = new AtomicInteger(0);
        int posionPillReceived = 0;
        Map<String, List<String>> errors = new HashMap<>();

        try (CSVPrinter printer = new CSVPrinterFactory().create(filename, header)){
            while (writing) {
                MessagePOJO message = queue.take();
                LOGGER.debug("Writing message: {}", message);

                if (!message.getIsPoisonPill()) {
                    if (message.getErrors() == null) {
                        printer.printRecord(message.getName(), message.getCount());
                        LOGGER.debug("Written message name {} and count {}", message.getName(), message.getCount());
                    } else {
                        errors.put("errors", message.getErrors());
                        printer.printRecord(message.getName(), message.getCount(), MAPPER.writeValueAsString(errors));
                        LOGGER.debug("Written message name {}, count {} and errors {}",
                                message.getName(), message.getCount(), errors);
                    }

                    messageCount.incrementAndGet();
                } else {
                    posionPillReceived++;
                    if (posionPillReceived == poisonPillCount) {
                        writing = false;
                    }
                }
            }
            LOGGER.info("Messages written: {}", messageCount);
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted", e);
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            LOGGER.error("Failed to do CSV writing for file {}", filename, e);
        }

        return messageCount.get();
    }
}
