package com.ecommerce.multivendor.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(String email, String role) {
        return buildToken(email, role, accessTokenExpiration, "access");
    }

    public String generateRefreshToken(String email, String role) {
        return buildToken(email, role, refreshTokenExpiration, "refresh");
    }

    private String buildToken(String subject, String role, long expiration, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        JwtBuilder builder = Jwts.builder()
                .setSubject(subject)
                .claim("type", tokenType)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    public String getEmailFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return (String) parseClaims(token).get("role");
    }

    public String getTokenType(String token) {
        return (String) parseClaims(token).get("type");
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    public Date getExpirationDateFromToken(String token) {
        return parseClaims(token).getExpiration();
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
