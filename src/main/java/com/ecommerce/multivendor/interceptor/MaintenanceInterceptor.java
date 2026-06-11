package com.ecommerce.multivendor.interceptor;

import com.ecommerce.multivendor.repository.GlobalSettingsRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class MaintenanceInterceptor implements HandlerInterceptor {

    private final GlobalSettingsRepository settingsRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // Only intercept public API calls, allow admin/seller/auth/payment paths
        if (path.startsWith("/api/products") || path.startsWith("/api/categories") || path.startsWith("/api/search")) {
            boolean maintenance = settingsRepository.findByKey("MAINTENANCE_MODE")
                    .map(s -> "true".equalsIgnoreCase(s.getValue()))
                    .orElse(false);

            if (maintenance) {
                response.setStatus(503);
                response.getWriter().write("{\"success\":false, \"message\":\"System is under maintenance. Please try again later.\", \"error\":\"MAINTENANCE\"}");
                response.setContentType("application/json");
                return false;
            }
        }

        return true;
    }
}

