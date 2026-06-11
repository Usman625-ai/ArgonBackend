package com.ecommerce.multivendor.security;

import com.ecommerce.multivendor.entity.User;
import com.ecommerce.multivendor.exception.UnauthorizedException;
import com.ecommerce.multivendor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Returns the currently authenticated user from the SecurityContext.
     *
     * Throws UnauthorizedException if:
     *  - No authentication in context  (no token sent)
     *  - Principal is "anonymousUser"  (token missing / invalid)
     *  - User email not in database    (corrupted token)
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("No authentication found. Please login.");
        }

        String email = authentication.getName();
        log.debug("SecurityUtils.getCurrentUser() → principal: [{}]", email);

        // anonymousUser = no JWT token was sent with the request
        if ("anonymousUser".equals(email)) {
            throw new UnauthorizedException(
                    "Authentication required. Please include your Bearer token in the Authorization header.");
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException(
                        "Authenticated user not found in database: " + email));
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new UnauthorizedException("No authenticated user.");
        }
        return auth.getName();
    }
}
