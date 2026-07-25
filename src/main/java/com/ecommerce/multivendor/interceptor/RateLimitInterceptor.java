package com.ecommerce.multivendor.interceptor;

import com.ecommerce.multivendor.security.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * IP-based rate limiting for the auth endpoints most likely to be abused:
 * brute-forcing a password, brute-forcing a 6-digit OTP, or spamming the
 * email-sending endpoints. Keyed by client IP + path, sliding window.
 *
 * NOTE: this is a coarse, first line of defense (per-IP). It does not
 * replace the per-email cooldowns already in AuthService for OTP resend.
 */
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitService rateLimitService;

    private record Limit(int maxRequests, long windowSeconds) {}

    // path (exact match on request URI) -> limit
    private static final Map<String, Limit> LIMITS = new LinkedHashMap<>();
    static {
        LIMITS.put("/api/auth/login", new Limit(10, 15 * 60));
        LIMITS.put("/api/auth/register", new Limit(5, 60 * 60));
        LIMITS.put("/api/auth/forgot-password", new Limit(5, 60 * 60));
        LIMITS.put("/api/auth/resend-otp", new Limit(5, 15 * 60));
        LIMITS.put("/api/auth/verify-email", new Limit(8, 15 * 60));
        LIMITS.put("/api/auth/change-password/request-otp", new Limit(5, 15 * 60));
        LIMITS.put("/api/auth/change-password", new Limit(8, 15 * 60));
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Limit limit = LIMITS.get(request.getRequestURI());
        if (limit == null) return true;

        String ip = clientIp(request);
        String key = request.getRequestURI() + ":" + ip;

        if (!rateLimitService.isAllowed(key, limit.maxRequests(), limit.windowSeconds())) {
            long retryAfter = rateLimitService.secondsUntilRetry(key, limit.windowSeconds());
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.getWriter().write(
                    "{\"success\":false,\"error\":\"Too many requests. Please try again in " + retryAfter + " second(s).\"}"
            );
            return false;
        }
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}