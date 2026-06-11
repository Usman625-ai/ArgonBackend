package com.ecommerce.multivendor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider        jwtTokenProvider;
    private final CustomUserDetailsService userDetailsService;
    private final TokenBlacklistService  tokenBlacklistService;

    @Override
    protected void doFilterInternal(HttpServletRequest  request,
                                    HttpServletResponse response,
                                    FilterChain         filterChain)
            throws ServletException, IOException {
        try {
            String token = extractToken(request);

            if (StringUtils.hasText(token)) {
                if (tokenBlacklistService.isBlacklisted(token)) {
                    log.trace("Blacklisted token rejected: {}", request.getRequestURI());
                    filterChain.doFilter(request, response);
                    return;
                }

                if (jwtTokenProvider.validateToken(token)) {
                    String tokenType = jwtTokenProvider.getTokenType(token);

                    if ("access".equals(tokenType)) {
                        String email = jwtTokenProvider.getEmailFromToken(token);
                        String role  = jwtTokenProvider.getRoleFromToken(token);

                        UsernamePasswordAuthenticationToken auth;
                        if (role != null) {
                            // OPTIMIZATION: Use roles from JWT to avoid DB hit
                            auth = new UsernamePasswordAuthenticationToken(
                                    email, null, java.util.Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_" + role)));
                        } else {
                            // Fallback to DB if role claim is missing (old tokens)
                            org.springframework.security.core.userdetails.UserDetails userDetails =
                                    userDetailsService.loadUserByUsername(email);
                            auth = new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                        }

                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        log.trace("JWT authenticated user: {} → {}",
                                email, request.getRequestURI());
                    } else {
                        log.trace("Refresh token rejected for non-refresh endpoint: {}",
                                request.getRequestURI());
                    }
                } else {
                    log.trace("Invalid JWT token for: {}", request.getRequestURI());
                }
            } else {
                log.trace("No Bearer token on: {} {}", request.getMethod(), request.getRequestURI());
            }

        } catch (Exception e) {
            log.error("JWT filter error on {}: {}", request.getRequestURI(), e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /** Extract raw token from "Authorization: Bearer <token>" header. */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        return null;
    }
}