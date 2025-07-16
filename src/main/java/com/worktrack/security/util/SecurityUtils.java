package com.worktrack.security.util;

import com.worktrack.exception.auth.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Authentication getAuthenticationForced() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new AuthenticationException("Security context was not properly initialized");
        }
        return auth;
    }
}