package com.ecommerce.multivendor.config;

import com.ecommerce.multivendor.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    // Public endpoints - no authentication needed
    /**
     * Explicitly listed public endpoints.
     * DO NOT use /api/auth/** — that would make change-password public too,
     * causing SecurityUtils.getCurrentUser() to get "anonymousUser".
     */
    private static final String[] PUBLIC_URLS = {
            // Auth — only endpoints that don't need a logged-in user
            "/api/auth/register",
            "/api/auth/login",
            "/api/auth/refresh-token",
            "/api/auth/verify-email",
            "/api/auth/resend-otp",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/logout",
            // Public product browsing
            "/api/products",
            "/api/products/**",
            "/api/categories",
            "/api/categories/**",
            "/api/search",
            "/api/brands",
            // JazzCash callbacks (no JWT, secured by pp_SecureHash instead)
            "/api/payments/jazzcash/callback",
            "/api/payments/jazzcash/verify",
            "/api/payments/jazzcash/test-hash",
            // Infra
            "/actuator/health",
            "/v3/api-docs/**",
            "/swagger-ui/**"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            new ObjectMapper().writeValue(res.getWriter(),
                                    Map.of("success", false, "error", "Unauthorized: " + e.getMessage()));
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            new ObjectMapper().writeValue(res.getWriter(),
                                    Map.of("success", false, "error", "Access Denied: Insufficient permissions"));
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        // ── Public — no token required ──────────────────────────────
                        .requestMatchers(PUBLIC_URLS).permitAll()

                        // ── Role-based ───────────────────────────────────────────────
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/seller/**").hasRole("SELLER")
                        .requestMatchers("/api/customer/**").hasRole("CUSTOMER")

                        // ── Everything else (incl. /api/auth/change-password) needs JWT
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000",
                "http://localhost:5173",
                "http://localhost:4173",
                "http://localhost:8080",
                "http://localhost:8081",
                "https://yourdomain.com"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept",
                "X-Requested-With", "Origin"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}