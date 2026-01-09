package com.worktrack.aspect;

import com.worktrack.entity.auth.Role;
import com.worktrack.security.util.SecurityUtils;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuthorizationAspect {
    @Before("execution(* com.worktrack.controller.AdminController.*(..))") // AdminController does not exist, Example Aspect for securing admin-only endpoints.
    public void checkIfUserIsAdmin() {
        var auth = SecurityUtils.getAuthenticationForced();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + Role.ADMIN.name()));

        if (!isAdmin) {
            throw new AccessDeniedException("You are either not authenticated or do not have the required permissions");
        }

    }
}