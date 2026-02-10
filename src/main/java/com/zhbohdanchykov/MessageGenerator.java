package com.zhbohdanchykov;

import com.github.javafaker.Faker;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class MessageGenerator {

    private static MessagePOJO msg = new MessagePOJO();

    public static MessagePOJO generateMessage() {
        msg.setName(generateRandomName());
        LocalDate birthDate = generateRandomDate();
        msg.setEddr(generateRandomEddr(birthDate));
        msg.setCount(generateRandomCount());
        msg.setCreated_at(generateRandomCreated_at(birthDate));
        return msg;
    }

    public static String generateRandomName() {
        Faker faker = new Faker();
        return  faker.name().firstName();
    }

    public static String generateRandomEddr(LocalDate birthDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String birthDateEddr = birthDate.format(formatter);
        String entryNumberEddr = String.format("%04d", ThreadLocalRandom.current().nextInt(1, 10000));
        String controlDigit = generateControlDigit(birthDateEddr + entryNumberEddr);
        StringBuilder builder = new StringBuilder();
        builder.append(birthDateEddr);
        builder.append("-");
        builder.append(entryNumberEddr);
        if (ThreadLocalRandom.current().nextBoolean()) {
            builder.append(controlDigit);
        } else {
            builder.append(ThreadLocalRandom.current().nextInt(0, 10));
        }
        return builder.toString();
    }

    private static String generateControlDigit(String eddr) {
        int sum = 0;
        int[] weights = {7, 3, 1};
        for (int i = 0; i < eddr.length(); i++) {
            sum += Integer.parseInt(String.valueOf(eddr.charAt(i))) * weights[i % weights.length];
        }
        return String.valueOf(sum % 10);
    }

    public static int generateRandomCount() {
        return ThreadLocalRandom.current().nextInt(0, 999);
    }

    public static LocalDateTime generateRandomCreated_at(LocalDate birthDate) {
        LocalDateTime start = birthDate.atStartOfDay();
        LocalDateTime end = LocalDateTime.now();
        long startSeconds = start.toEpochSecond(ZoneOffset.UTC);
        long endSeconds = end.toEpochSecond(ZoneOffset.UTC);
        long randomSeconds = ThreadLocalRandom.current().nextLong(startSeconds, endSeconds);
        return LocalDateTime.ofEpochSecond(randomSeconds, 0, ZoneOffset.UTC);
    }

    public static LocalDate generateRandomDate() {
        LocalDate start = LocalDate.of(1900, 1, 1);
        LocalDate end = LocalDate.now();
        long startDays = start.toEpochDay();
        long endDays = end.toEpochDay();
        long randomDays = ThreadLocalRandom.current().nextLong(startDays, endDays);
        return LocalDate.ofEpochDay(randomDays);
    }
}
