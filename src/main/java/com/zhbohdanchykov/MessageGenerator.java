package com.zhbohdanchykov;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

public class MessageGenerator {

    private static final char[] LETTERS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final DateTimeFormatter EDDR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private MessageGenerator() {}

    public static MessagePOJO generate() {
        MessagePOJO msg = new MessagePOJO();
        msg.setName(generateRandomName());
        LocalDateTime now = LocalDateTime.now();
        LocalDate birthDate = generateRandomDateTime(
                LocalDateTime.of(1900, 1, 1, 0, 0), now).toLocalDate();
        msg.setEddr(generateRandomEddr(birthDate));
        msg.setCount(generateRandomCount());
        msg.setCreatedAt(generateRandomDateTime(birthDate.atStartOfDay(), now));
        return msg;
    }

    public static String generateRandomName() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int length = random.nextInt(2, 15);

        char[] name = new char[length];

        name[0] = Character.toUpperCase(LETTERS[random.nextInt(LETTERS.length)]);
        for (int i = 1; i < length; i++) {
            name[i] = LETTERS[random.nextInt(LETTERS.length)];
        }

        return new String(name);
    }

    public static String generateRandomEddr(LocalDate birthDate) {
        String birthDateEddr = birthDate.format(EDDR_FORMATTER);
        String entryNumberEddr = String.format("%04d", ThreadLocalRandom.current().nextInt(1, 10000));
        String controlDigit;
        if (ThreadLocalRandom.current().nextBoolean()) {
            controlDigit = generateControlDigit(birthDateEddr);
        } else {
            controlDigit = String.valueOf(ThreadLocalRandom.current().nextInt(0, 10));
        }
        return birthDateEddr + "-" + entryNumberEddr + controlDigit;
    }

    private static String generateControlDigit(String eddr) {
        int sum = 0;
        int[] weights = {7, 3, 1};
        for (int i = 0; i < eddr.length(); i++) {
            sum += (eddr.charAt(i) - '0') * weights[i % weights.length];
        }
        return String.valueOf(sum % 10);
    }

    public static int generateRandomCount() {
        return ThreadLocalRandom.current().nextInt(0, 999);
    }

    public static LocalDateTime generateRandomDateTime(LocalDateTime start, LocalDateTime end) {
        long startSeconds = start.toEpochSecond(ZoneOffset.UTC);
        long endSeconds = end.toEpochSecond(ZoneOffset.UTC);
        long randomSeconds = ThreadLocalRandom.current().nextLong(startSeconds, endSeconds);
        return LocalDateTime.ofEpochSecond(randomSeconds, 0, ZoneOffset.UTC);
    }
}
