package com.ecommerce.multivendor.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JazzCash payment gateway configuration.
 */

@Configuration
@Getter
public class JazzCashConfig {

    @Value("${jazzcash.merchant-id}")
    private String merchantId;

    @Value("${jazzcash.password}")
    private String password;

    /** Integrity Salt is used as the HMAC-SHA256 key for secure hash generation. */
    @Value("${jazzcash.integrity-salt}")
    private String integritySalt;

    /** The URL JazzCash will POST the payment result back to. */
    @Value("${jazzcash.return-url}")
    private String returnUrl;

    @Value("${jazzcash.currency:PKR}")
    private String currency;

    @Value("${jazzcash.language:EN}")
    private String language;

    @Value("${jazzcash.sandbox:true}")
    private boolean sandbox;

    @Value("${jazzcash.sandbox-api-url}")
    private String sandboxApiUrl;

    @Value("${jazzcash.live-api-url}")
    private String liveApiUrl;

    @Value("${jazzcash.hosted-sandbox-url}")
    private String hostedSandboxUrl;

    @Value("${jazzcash.hosted-live-url}")
    private String hostedLiveUrl;

    /** Returns the correct REST API endpoint based on sandbox flag. */
    public String getApiUrl() {
        return sandbox ? sandboxApiUrl : liveApiUrl;
    }

    /** Returns the correct hosted-page URL for browser redirect. */
    public String getHostedPageUrl() {
        return sandbox ? hostedSandboxUrl : hostedLiveUrl;
    }
}
