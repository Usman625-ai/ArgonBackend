package com.ecommerce.multivendor.service.impl;

import com.ecommerce.multivendor.config.JazzCashConfig;
import com.ecommerce.multivendor.dto.request.PaymentVerifyRequest;
import com.ecommerce.multivendor.dto.response.JazzCashCallbackResponse;
import com.ecommerce.multivendor.dto.response.PaymentOrderResponse;
import com.ecommerce.multivendor.entity.Order;
import com.ecommerce.multivendor.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * JazzCash Payment Service — Hosted Payment Page flow.
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │  Sandbox registration → https://sandbox.jazzcash.com.pk/    │
 * │  You receive: Merchant ID, Password, Integrity Salt          │
 * │  Put them in your .env file.                                 │
 * └──────────────────────────────────────────────────────────────┘
 *
 * Flow:
 *  1. POST /api/customer/orders/{id}/payment/initiate
 *     → backend builds signed params, returns { hostedPageUrl, formParams }
 *
 *  2. Frontend creates a hidden form → POST to hostedPageUrl
 *     → user lands on JazzCash checkout (card / mobile wallet)
 *
 *  3. JazzCash POSTs callback to pp_ReturnURL
 *     → GET /api/payments/jazzcash/callback?pp_ResponseCode=000&...
 *     → backend verifies pp_SecureHash → marks order PAID
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final JazzCashConfig config;

    /**
     * Shared OkHttp client for all outbound JazzCash REST API calls.
     * Reusing a single instance is best practice (thread-safe, connection pool).
     */
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build();

    private static final String SUCCESS_CODE  = "000";
    private static final DateTimeFormatter DT_FMT =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ─────────────────────────────────────────────────────────────
    // 1. Initiate Payment — Build Signed Form Params
    // ─────────────────────────────────────────────────────────────

    public PaymentOrderResponse createPayment(Order order) {
        try {
            String txnRefNo    = buildTxnRefNo(order.getOrderNumber());
            String txnDateTime = LocalDateTime.now(ZoneId.of("Asia/Karachi")).format(DT_FMT);
            String txnExpiry   = LocalDateTime.now(ZoneId.of("Asia/Karachi"))
                                              .plusMinutes(30).format(DT_FMT);

            // JazzCash amounts are in PAISA  (PKR 1500.00 → "150000")
            long amountPaisa = order.getFinalAmount()
                                    .multiply(BigDecimal.valueOf(100))
                                    .longValue();
            String amountStr = String.valueOf(amountPaisa);

            // TreeMap keeps keys sorted — required for deterministic hash
            Map<String, String> params = new TreeMap<>();
            params.put("pp_Version",           "1.1");
            params.put("pp_TxnType",           "MWALLET");
            params.put("pp_Language",          config.getLanguage());
            params.put("pp_MerchantID",        config.getMerchantId());
            params.put("pp_SubMerchantID",     "");
            params.put("pp_Password",          config.getPassword());
            params.put("pp_BankID",            "TBANK");
            params.put("pp_ProductID",         "RETL");
            params.put("pp_TxnRefNo",          txnRefNo);
            params.put("pp_Amount",            amountStr);
            params.put("pp_TxnCurrency",       config.getCurrency());
            params.put("pp_TxnDateTime",       txnDateTime);
            params.put("pp_TxnExpiryDateTime", txnExpiry);
            params.put("pp_BillReference",     order.getOrderNumber());
            params.put("pp_Description",       "Order #" + order.getOrderNumber()
                                               + " - " + order.getCustomer().getName());
            params.put("pp_ReturnURL",         config.getReturnUrl());

            // Compute and append the secure hash
            String secureHash = computeSecureHash(params);
            params.put("pp_SecureHash", secureHash);

            log.info("[JazzCash] Payment initiated | Order:{} | TxnRef:{} | PKR:{}",
                order.getOrderNumber(), txnRefNo, order.getFinalAmount());

            return PaymentOrderResponse.builder()
                .orderNumber(order.getOrderNumber())
                .txnRefNo(txnRefNo)
                .amount(order.getFinalAmount())
                .amountInPaisa(amountStr)
                .currency(config.getCurrency())
                .hostedPageUrl(config.getHostedPageUrl())
                .formParams(params)
                .build();

        } catch (Exception e) {
            log.error("[JazzCash] createPayment failed for order {}: {}",
                order.getOrderNumber(), e.getMessage());
            throw new BadRequestException("Payment gateway error: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 2. Verify Callback — Validate pp_SecureHash
    // ─────────────────────────────────────────────────────────────

    /**
     * Called when JazzCash redirects the user back to pp_ReturnURL.
     * Re-computes the secure hash and compares with pp_SecureHash.
     * NEVER trust pp_ResponseCode without hash verification.
     */
    public JazzCashCallbackResponse verifyCallback(Map<String, String> allParams) {
        String receivedHash = allParams.get("pp_SecureHash");
        if (receivedHash == null || receivedHash.isBlank()) {
            log.warn("[JazzCash] Callback with missing pp_SecureHash");
            return buildFailedCallback("Missing secure hash", allParams);
        }

        // Remove pp_SecureHash before recomputing
        Map<String, String> paramsToHash = new TreeMap<>(allParams);
        paramsToHash.remove("pp_SecureHash");

        String expectedHash;
        try {
            expectedHash = computeSecureHash(paramsToHash);
        } catch (Exception e) {
            log.error("[JazzCash] Hash computation error: {}", e.getMessage());
            return buildFailedCallback("Hash computation error", allParams);
        }

        if (!expectedHash.equalsIgnoreCase(receivedHash)) {
            log.warn("[JazzCash] HASH MISMATCH — possible tampering! " +
                     "Expected:{} Received:{}", expectedHash, receivedHash);
            return buildFailedCallback("Secure hash mismatch", allParams);
        }

        String code      = allParams.getOrDefault("pp_ResponseCode", "999");
        boolean success  = SUCCESS_CODE.equals(code);
        log.info("[JazzCash] Callback OK | TxnRef:{} | Code:{} | Success:{}",
            allParams.get("pp_TxnRefNo"), code, success);

        return JazzCashCallbackResponse.builder()
            .success(success)
            .responseCode(code)
            .responseMessage(allParams.getOrDefault("pp_ResponseMessage", ""))
            .txnRefNo(allParams.getOrDefault("pp_TxnRefNo", ""))
            .transactionId(allParams.getOrDefault("pp_TransactionId", ""))
            .amount(allParams.getOrDefault("pp_Amount", "0"))
            .txnDateTime(allParams.getOrDefault("pp_TxnDateTime", ""))
            .orderNumber(allParams.getOrDefault("pp_BillReference", ""))
            .build();
    }

    /** Overload for the /verify endpoint (frontend posts DTO). */
    public JazzCashCallbackResponse verifyCallback(PaymentVerifyRequest req) {
        Map<String, String> p = new TreeMap<>();
        p.put("pp_TxnRefNo",       safe(req.getPp_TxnRefNo()));
        p.put("pp_ResponseCode",   safe(req.getPp_ResponseCode()));
        p.put("pp_ResponseMessage",safe(req.getPp_ResponseMessage()));
        p.put("pp_TransactionId",  safe(req.getPp_TransactionId()));
        p.put("pp_Amount",         safe(req.getPp_Amount()));
        p.put("pp_TxnDateTime",    safe(req.getPp_TxnDateTime()));
        p.put("pp_SecureHash",     safe(req.getPp_SecureHash()));
        return verifyCallback(p);
    }

    // ─────────────────────────────────────────────────────────────
    // 3b. Refund — JazzCash DoRefundTransaction API
    // ─────────────────────────────────────────────────────────────

    /**
     * Initiates a refund for a previously successful JazzCash payment.
     *
     * Called from OrderService.cancelOrder() when:
     *   paymentStatus == PAID  AND  jazzCashTxnRefNo is not null
     *
     * @param originalTxnRefNo  order.getJazzCashTxnRefNo()   ← pp_TxnRefNo of original payment
     * @param amount            refund amount in PKR (e.g. new BigDecimal("1500.00"))
     * @return                  new refund TxnRefNo on success, null on failure
     */
    public String initiateRefund(String originalTxnRefNo, BigDecimal amount) {
        if (originalTxnRefNo == null || originalTxnRefNo.isBlank()) {
            log.warn("[JazzCash Refund] Cannot refund — originalTxnRefNo is blank");
            return null;
        }

        try {
            String refundTxnRef = buildTxnRefNo("R" + System.currentTimeMillis());
            String txnDateTime  = LocalDateTime.now(ZoneId.of("Asia/Karachi")).format(DT_FMT);
            long   paisa        = amount.multiply(BigDecimal.valueOf(100)).longValue();

            Map<String, String> params = new TreeMap<>();
            params.put("pp_Version",              "1.1");
            params.put("pp_TxnType",              "MWALLET");
            params.put("pp_Language",             config.getLanguage());
            params.put("pp_MerchantID",           config.getMerchantId());
            params.put("pp_SubMerchantID",        "");
            params.put("pp_Password",             config.getPassword());
            params.put("pp_TxnRefNo",             refundTxnRef);
            params.put("pp_Amount",               String.valueOf(paisa));
            params.put("pp_TxnCurrency",          config.getCurrency());
            params.put("pp_TxnDateTime",          txnDateTime);
            params.put("pp_TxnRefNoToBeRefunded", originalTxnRefNo); // ← key JazzCash field

            params.put("pp_SecureHash", computeSecureHash(params));

            // Build JSON body
            StringBuilder json = new StringBuilder("{");
            params.forEach((k, v) ->
                json.append("\"").append(k).append("\":\"").append(v).append("\","));
            json.deleteCharAt(json.length() - 1); // remove trailing comma
            json.append("}");

            String url = config.isSandbox()
                ? "https://sandbox.jazzcash.com.pk/ApplicationAPI/API/2.0/DoRefundTransaction"
                : "https://payments.jazzcash.com.pk/ApplicationAPI/API/2.0/DoRefundTransaction";

            Request req = new Request.Builder()
                .url(url)
                .post(RequestBody.create(json.toString(),
                    MediaType.get("application/json; charset=utf-8")))
                .addHeader("Content-Type", "application/json")
                .build();

            try (Response res = httpClient.newCall(req).execute()) {
                String body = res.body() != null ? res.body().string() : "";
                log.info("[JazzCash Refund] HTTP:{} | Body:{}", res.code(), body);

                if (res.isSuccessful() && body.contains("\"pp_ResponseCode\":\"000\"")) {
                    log.info("[JazzCash Refund] SUCCESS | Ref:{} → RefundRef:{} | PKR:{}",
                        originalTxnRefNo, refundTxnRef, amount);
                    return refundTxnRef;
                }
                log.warn("[JazzCash Refund] FAILED | Ref:{} | Response:{}", originalTxnRefNo, body);
                return null;
            }

        } catch (Exception e) {
            // Never throw — refund failure must NOT block order cancellation.
            // The merchant can manually refund via JazzCash merchant portal.
            log.error("[JazzCash Refund] Exception for txnRef {}: {}", originalTxnRefNo, e.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // 3. HMAC-SHA256 Secure Hash
    // ─────────────────────────────────────────────────────────────

    /**
     * JazzCash secure hash algorithm (from official API guide v2.0 §3.4):
     *
     *   hashString = integritySalt
     *                + "&" + sorted_non_empty_value_1
     *                + "&" + sorted_non_empty_value_2
     *                + ...
     *
     *   secureHash = HmacSHA256(key=integritySalt, data=hashString)
     *                converted to lowercase hex string
     */
    public String computeSecureHash(Map<String, String> params) throws Exception {
        // Sort keys alphabetically (TreeMap already does this, but ensure it)
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);

        StringBuilder sb = new StringBuilder(config.getIntegritySalt());
        for (String key : keys) {
            String val = params.getOrDefault(key, "");
            if (val != null && !val.isEmpty()) {
                sb.append("&").append(val);
            }
        }

        String message = sb.toString();
        log.debug("[JazzCash] SecureHash input: {}", message);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(
            config.getIntegritySalt().getBytes(StandardCharsets.UTF_8),
            "HmacSHA256"
        ));
        byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));

        StringBuilder hex = new StringBuilder();
        for (byte b : raw) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Helpers
    // ─────────────────────────────────────────────────────────────

    /**
     * Unique txn reference: max 20 chars.
     * "T" + 13-char order suffix + 6-char millis tail
     */
    private String buildTxnRefNo(String orderNumber) {
        String base = "T" + orderNumber.replaceAll("[^0-9]", "");
        String ts   = String.valueOf(System.currentTimeMillis());
        String ref  = base + ts.substring(ts.length() - 6);
        return ref.length() > 20 ? ref.substring(ref.length() - 20) : ref;
    }

    private JazzCashCallbackResponse buildFailedCallback(
            String reason, Map<String, String> p) {
        return JazzCashCallbackResponse.builder()
            .success(false)
            .responseCode(p.getOrDefault("pp_ResponseCode", "999"))
            .responseMessage(reason)
            .txnRefNo(p.getOrDefault("pp_TxnRefNo", ""))
            .orderNumber(p.getOrDefault("pp_BillReference", ""))
            .build();
    }

    private String safe(String s) { return s != null ? s : ""; }
}
