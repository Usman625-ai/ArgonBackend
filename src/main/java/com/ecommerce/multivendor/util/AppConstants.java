package com.ecommerce.multivendor.util;

public class AppConstants {
    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 12;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIR = "desc";

    public static final int OTP_EXPIRY_MINUTES = 10;
    public static final int PASSWORD_RESET_EXPIRY_HOURS = 24;
    public static final int ORDER_CANCELLATION_WINDOW_HOURS = 24;
    public static final int LOW_STOCK_THRESHOLD = 10;

    // Order number prefix
    public static final String ORDER_PREFIX = "ORD";

    private AppConstants() {}
}
