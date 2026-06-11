package com.ecommerce.multivendor.enums;

public enum PaymentMethod {
    CASH_ON_DELIVERY,
    JAZZCASH,       // JazzCash mobile wallet / card — works in Pakistan
    BANK_TRANSFER,  // Manual bank / EasyPaisa over-the-counter
    WALLET          // In-app wallet balance
}
