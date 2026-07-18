package com.ecommerce.multivendor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@EnableCaching
@ComponentScan(basePackages = "com.ecommerce.multivendor")
public class MultiVendorEcommerceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MultiVendorEcommerceApplication.class, args);
    }
}
//        in admin,category creation layout is covering full page,in seller updating product show only parent categories not child category also in customer website products section is not looking good change that layout,also product deletion should delete product peramanently from database and its images at cloudinary also when a rejected seller reapplied send notification to admin also that this seller has reapplied review it