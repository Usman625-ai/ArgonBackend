package com.ecommerce.multivendor.config;

import com.ecommerce.multivendor.interceptor.MaintenanceInterceptor;
import com.ecommerce.multivendor.interceptor.RateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final MaintenanceInterceptor maintenanceInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor);
        registry.addInterceptor(maintenanceInterceptor);
    }
}

