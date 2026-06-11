package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Returned to the frontend after initiating a JazzCash payment.
 *
 * Frontend flow:
 *   1. Receive this response.
 *   2. Create a hidden HTML form targeting {@code hostedPageUrl}.
 *   3. Add every entry in {@code formParams} as a hidden <input>.
 *   4. Submit the form — user lands on JazzCash checkout.
 *   5. After payment JazzCash POSTs back to pp_ReturnURL on our server.
 */
@Data
@Builder
public class PaymentOrderResponse {
    private String orderNumber;
    private String txnRefNo;          // pp_TxnRefNo we generated
    private BigDecimal amount;        // PKR amount for display
    private String amountInPaisa;     // amount * 100 as string
    private String currency;
    private String hostedPageUrl;     // Form POST target
    private Map<String, String> formParams;  // All signed pp_ fields
}
