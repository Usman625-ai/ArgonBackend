package com.ecommerce.multivendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

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
        // Must run before SpringApplication.run() — Hibernate's
        // EntityManagerFactory (built during context startup) can read/cache
        // the JVM's default timezone earlier than any @PostConstruct bean
        // would fire, which is why setting it there didn't take effect.
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Karachi"));
        SpringApplication.run(MultiVendorEcommerceApplication.class, args);
    }
}