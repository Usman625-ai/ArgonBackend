package com.ecommerce.multivendor.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Caches are read-mostly, low-cardinality lookups (category tree, brand
 * list) — small enough to live in-process, but bounded so they can't grow
 * unbounded and stale enough data doesn't linger forever.
 *
 * "categories" is also explicitly evicted on admin writes (see
 * CategoryService), so its TTL here is just a safety net.
 * "brands" has no explicit eviction hook (it's derived from product rows
 * scattered across seller/admin product CRUD), so it relies on the TTL
 * below to pick up new/removed brands within a few minutes.
 */
@Configuration
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("categories", "brands");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500));
        return manager;
    }
}
