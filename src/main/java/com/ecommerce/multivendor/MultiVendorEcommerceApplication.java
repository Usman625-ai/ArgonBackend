package com.ecommerce.multivendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
// order = LOWEST_PRECEDENCE - 1 makes the retry advisor wrap OUTSIDE the (default
// LOWEST_PRECEDENCE) transaction advisor, so each @RetryableRead retry attempt opens
// a genuinely new transaction/connection instead of retrying inside the same one
// that just failed.
@EnableRetry(order = Ordered.LOWEST_PRECEDENCE - 1)
@ComponentScan(basePackages = "com.ecommerce.multivendor")
public class MultiVendorEcommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultiVendorEcommerceApplication.class, args);
    }
}