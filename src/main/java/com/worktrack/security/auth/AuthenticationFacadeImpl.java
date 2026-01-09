package com.worktrack.security.auth;

import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.auth.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFacadeImpl implements AuthenticationFacade {
    @Override
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public String getCurrentUsername() {
        Authentication auth = getAuthentication();
        if (isNotAuthenticated(auth)) {
            throw new AuthenticationException("No authenticated user found in security context.");
        }
        return auth.getName();
    }

    @Override
    public Long getCurrentUserId() {
        Authentication auth = getAuthentication();
        if (isNotAuthenticated(auth) || auth.getPrincipal() == null) {
            throw new AuthenticationException("No authenticated user found in security context.");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        throw new AuthenticationException("Cannot determine current user id. Principal is not a valid User instance.");
    }


    @Override
    public boolean hasRole(Role role) {
        Authentication auth = getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getAuthorities() == null) {
            return false;
        }
        String expected = "ROLE_" + role.name();
        return auth.getAuthorities().stream()
                .anyMatch(a -> expected.equals(a.getAuthority()));
    }

    private boolean isNotAuthenticated(Authentication auth) {
        return auth == null || !auth.isAuthenticated();
    }

}
