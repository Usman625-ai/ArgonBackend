package com.ecommerce.multivendor.controller;

import com.ecommerce.multivendor.dto.response.ApiResponse;
import com.ecommerce.multivendor.dto.response.JazzCashCallbackResponse;
import com.ecommerce.multivendor.service.impl.OrderService;
import com.ecommerce.multivendor.service.impl.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * JazzCash Payment Callback Controller
 * JazzCash calls this URL after the customer completes (or fails)
 * payment on the hosted checkout page.

 * The return URL must be:
 *   • Publicly accessible (not localhost) in production.
 *   • For local dev use ngrok:  ngrok http 8080
 *     then set JAZZCASH_RETURN_URL=https://xxxx.ngrok.io/api/payments/jazzcash/callback

 * JazzCash sends ALL pp_ fields as HTTP POST body parameters.
 * We verify pp_SecureHash then update the order.

 * NOTE: This endpoint is intentionally PUBLIC (no JWT auth)
 * because JazzCash calls it server-to-server, not the customer browser.
 * Security is ensured by pp_SecureHash verification.

 */
@RestController
@RequestMapping("/api/payments/jazzcash")
@RequiredArgsConstructor
@Slf4j
public class PaymentCallbackController {

    private final PaymentService paymentService;
    private final OrderService   orderService;

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    // 1.  Server-to-Server Callback (JazzCash → Our Server)

    /**
     * JazzCash POSTs all pp_ parameters here after payment.
     *
     * Set JAZZCASH_RETURN_URL=https://xxxx.ngrok.io/api/payments/jazzcash/callback
     *
     * JazzCash sends:
     *   pp_ResponseCode    "000" = success, anything else = failure
     *   pp_TransactionId   JazzCash unique transaction ID
     *   pp_TxnRefNo        Our reference (we set this during initiation)
     *   pp_BillReference   Our order number
     *   pp_Amount          Amount in paisa
     *   pp_SecureHash      HMAC-SHA256 — MUST be verified
     *   ...and more fields
     */
    @PostMapping("/callback")
    public ResponseEntity<String> handleCallback(HttpServletRequest httpRequest) {
        // Extract all request parameters (JazzCash sends form POST)
        Map<String, String> params = extractParams(httpRequest);
        log.info("[JazzCash Callback] Received params: {}", maskSensitive(params));

        // Verify + process
        JazzCashCallbackResponse result = paymentService.verifyCallback(params);
        orderService.processJazzCashCallback(result);

        // JazzCash expects a 200 OK with any body
        return ResponseEntity.ok("OK");
    }

    /**
     * Alternative: JazzCash sometimes does a GET redirect with query params.
     * Handle both to be safe.
     */
    @GetMapping("/callback")
    public ResponseEntity<Void> handleCallbackGet(
            @RequestParam Map<String, String> params) {
        log.info("[JazzCash Callback GET] Received params: {}", maskSensitive(params));

        JazzCashCallbackResponse result = paymentService.verifyCallback(params);
        orderService.processJazzCashCallback(result);

        // Redirect customer to frontend order page
        String orderNum = result.getOrderNumber();
        String location = result.isSuccess()
            ? frontendUrl + "/orders?payment=success&ref=" + orderNum
            : frontendUrl + "/orders?payment=failed&ref=" + orderNum;

        return ResponseEntity.status(302)
            .header("Location", location)
            .build();
    }

    // 2.  Manual Verify (Frontend → Our Backend)

    /**
     * Called by the frontend when it receives JazzCash redirect params.

     * Use-case: The customer's browser is redirected back to your site with
     * all pp_ params as query string. Frontend sends them here to verify
     * and get the updated order status.

     * POST /api/payments/jazzcash/verify
     * Body: { "pp_ResponseCode": "000", "pp_SecureHash": "...", ... }
     */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<JazzCashCallbackResponse>> verifyPayment(
            @RequestBody Map<String, String> params) {
        log.info("[JazzCash Verify] Request params: {}", maskSensitive(params));

        JazzCashCallbackResponse result = paymentService.verifyCallback(params);
        orderService.processJazzCashCallback(result);

        String msg = result.isSuccess()
            ? "Payment verified successfully"
            : "Payment verification failed: " + result.getResponseMessage();

        return ResponseEntity.ok(ApiResponse.<JazzCashCallbackResponse>builder()
            .success(result.isSuccess())
            .message(msg)
            .data(result)
            .build());
    }

    // 3.  Hash Test Utility (DEV only — remove in production)

    /**
     * Utility endpoint to test your secure hash computation.
     * Remove or secure this endpoint in production.

     * POST /api/payments/jazzcash/test-hash
     * Body: { "pp_Amount": "150000", "pp_TxnRefNo": "T2024001", ... }
     */
    @PostMapping("/test-hash")
    public ResponseEntity<ApiResponse<Map<String, String>>> testHash(
            @RequestBody Map<String, String> params) {
        try {
            String hash = paymentService.computeSecureHash(params);
            return ResponseEntity.ok(ApiResponse.success(
                Map.of("pp_SecureHash", hash, "paramsUsed", params.toString())
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("Hash error: " + e.getMessage()));
        }
    }

    // Helpers

    /** Extract all form POST parameters from HttpServletRequest. */
    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();
        request.getParameterMap().forEach((k, values) -> {
            if (values != null && values.length > 0) {
                map.put(k, values[0]);
            }
        });
        return map;
    }

    /** Mask pp_Password and pp_SecureHash in logs to avoid leaking secrets. */
    private Map<String, String> maskSensitive(Map<String, String> params) {
        Map<String, String> masked = new HashMap<>(params);
        masked.replaceAll((k, v) -> {
            if (k.equalsIgnoreCase("pp_Password") ||
                k.equalsIgnoreCase("pp_SecureHash")) {
                return v != null && v.length() > 8
                    ? v.substring(0, 4) + "****" + v.substring(v.length() - 4)
                    : "****";
            }
            return v;
        });
        return masked;
    }
}
