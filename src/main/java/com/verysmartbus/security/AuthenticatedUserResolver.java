package com.verysmartbus.security;

import com.verysmartbus.exception.ForbiddenOperationException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Single place that extracts the authenticated application's user ID.
 */
@Component
public class AuthenticatedUserResolver {

    public Long getCurrentUserId(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new ForbiddenOperationException("No authenticated application user found on this request.");
        }
        return principal.getUserId();
    }
}
