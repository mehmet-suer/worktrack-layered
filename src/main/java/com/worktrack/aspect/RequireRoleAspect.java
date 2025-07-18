package com.worktrack.aspect;

import com.worktrack.annotation.RequireRole;
import com.worktrack.security.util.SecurityUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;



@Aspect
@Component
public class RequireRoleAspect {
    private static final Logger logger = LoggerFactory.getLogger(RequireRoleAspect.class);
    @Before("@annotation(requireRole)")
    public void checkUserRole(RequireRole requireRole) {
        var auth = SecurityUtils.getAuthenticationForced();
        Set<String> expectedAuthorities = getExpectedAuthorities(requireRole);

        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(authority -> expectedAuthorities.contains(authority.getAuthority()));

        if (!hasRole) {
            String msg = requireRole.message();
            String errorMessage = (msg != null && !msg.trim().isEmpty())
                    ? msg
                    : "You must have at least one of the following roles to perform this action: " +
                    String.join(", ", expectedAuthorities);

            throw new AccessDeniedException(errorMessage);
        }
    }

    private static Set<String> getExpectedAuthorities(RequireRole requireRole) {
        String[] requiredRoles =  requireRole.value();
        return Stream.of(requiredRoles)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());
    }
}
