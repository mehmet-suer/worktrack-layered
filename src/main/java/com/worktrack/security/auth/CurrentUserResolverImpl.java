package com.worktrack.security.auth;

import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.auth.AuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolverImpl implements CurrentUserResolver {
    @Override
    public Long getCurrentUserId() {
        Authentication authentication = getAuthentication();
        if (isNotAuthenticated(authentication) || authentication.getPrincipal() == null) {
            throw new AuthenticationException("No authenticated user found in security context.");
        }
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new AuthenticationException("Cannot determine current user id. Principal is not a valid User instance.");
    }

    @Override
    public boolean hasRole(Role role) {
        Authentication authentication = getAuthentication();
        if (isNotAuthenticated(authentication) || authentication.getAuthorities() == null) {
            return false;
        }
        String expected = "ROLE_" + role.name();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> expected.equals(authority.getAuthority()));
    }

    private boolean isNotAuthenticated(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated();
    }

    private Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
