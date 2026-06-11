package com.ecommerce.multivendor.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

public class OrderNumberGenerator {
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String generate() {
        String date = LocalDateTime.now().format(FORMATTER);
        int seq = counter.incrementAndGet() % 10000;
        return String.format("%s%s%04d", AppConstants.ORDER_PREFIX, date, seq);
    }

    private OrderNumberGenerator() {}
}
