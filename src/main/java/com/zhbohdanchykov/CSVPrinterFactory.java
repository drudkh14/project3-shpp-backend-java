package com.zhbohdanchykov;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class CSVPrinterFactory {

    public CSVPrinter create(String fileName, String[] header) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader(header)
                .setSkipHeaderRecord(false)
                .get();

        return new CSVPrinter(
                Files.newBufferedWriter(
                        Paths.get(fileName),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                ),
                format
        );
    }
}
