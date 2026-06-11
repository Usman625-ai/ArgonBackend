package com.ecommerce.multivendor.security;

import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Date;

@Service
public class TokenBlacklistService {
    // Stores blacklisted tokens and their expiry date
    private final Map<String, Date> blacklist = new ConcurrentHashMap<>();

    public void blacklistToken(String token, Date expiryDate) {
        blacklist.put(token, expiryDate);
    }

    public boolean isBlacklisted(String token) {
        Date expiry = blacklist.get(token);
        if (expiry == null) {
            return false;
        }
        // If expired, clean it up and return false
        if (expiry.before(new Date())) {
            blacklist.remove(token);
            return false;
        }
        return true;
    }

    // Optional: Add a cleanup task to remove expired tokens regularly
}

