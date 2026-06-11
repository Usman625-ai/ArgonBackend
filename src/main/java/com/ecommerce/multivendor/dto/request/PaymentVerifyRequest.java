package com.ecommerce.multivendor.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Sent by the frontend after JazzCash redirects back with all pp_ params.
 * @JsonProperty is required because Jackson maps camelCase by default.
 * JazzCash sends field names with underscores (pp_TxnRefNo),
 * so we map them explicitly.
 */
@Data
public class PaymentVerifyRequest {

    @NotBlank
    @JsonProperty("pp_TxnRefNo")
    private String pp_TxnRefNo;

    @NotBlank
    @JsonProperty("pp_ResponseCode")
    private String pp_ResponseCode;      // "000" = success

    @JsonProperty("pp_ResponseMessage")
    private String pp_ResponseMessage;

    @JsonProperty("pp_TransactionId")
    private String pp_TransactionId;     // JazzCash transaction ID

    @JsonProperty("pp_Amount")
    private String pp_Amount;            // Amount in paisa

    @JsonProperty("pp_TxnDateTime")
    private String pp_TxnDateTime;

    @NotBlank
    @JsonProperty("pp_SecureHash")
    private String pp_SecureHash;        // HMAC-SHA256 to verify
}
