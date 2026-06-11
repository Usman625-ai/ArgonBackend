package com.ecommerce.multivendor.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Internal DTO representing a parsed JazzCash payment callback.
 * Used after verifying pp_SecureHash.
 */
@Data
@Builder
public class JazzCashCallbackResponse {
    private boolean success;
    private String responseCode;      // "000" = success
    private String responseMessage;
    private String txnRefNo;
    private String transactionId;
    private String amount;
    private String txnDateTime;
    private String orderNumber;       // extracted from pp_BillReference
}
