package com.ecommerce.multivendor.security;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory sliding-window rate limiter.
 *
 * NOTE: like TokenBlacklistService, this is per-instance state — fine for a
 * single-server deployment (which is what this project targets). If you ever
 * scale to multiple instances behind a load balancer, move this to Redis
 * (e.g. Bucket4j + Redis) so limits are shared across instances.
 */
@Service
public class RateLimitService {

    private final ConcurrentHashMap<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /**
     * @param key           unique key for the thing being limited, e.g. "login:1.2.3.4:user@x.com"
     * @param maxRequests   max allowed requests within the window
     * @param windowSeconds size of the sliding window, in seconds
     * @return true if the request is allowed (and is recorded), false if the limit was hit
     */
    public boolean isAllowed(String key, int maxRequests, long windowSeconds) {
        long now = System.currentTimeMillis();
        long windowStart = now - (windowSeconds * 1000);

        Deque<Long> timestamps = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (timestamps) {
            while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                timestamps.pollFirst();
            }
            if (timestamps.size() >= maxRequests) {
                return false;
            }
            timestamps.addLast(now);
            return true;
        }
    }

    /** Seconds until the caller can try again, based on the oldest hit in the current window. */
    public long secondsUntilRetry(String key, long windowSeconds) {
        Deque<Long> timestamps = hits.get(key);
        if (timestamps == null || timestamps.isEmpty()) return 0;
        synchronized (timestamps) {
            Long oldest = timestamps.peekFirst();
            if (oldest == null) return 0;
            long elapsed = (System.currentTimeMillis() - oldest) / 1000;
            return Math.max(0, windowSeconds - elapsed);
        }
    }
}
