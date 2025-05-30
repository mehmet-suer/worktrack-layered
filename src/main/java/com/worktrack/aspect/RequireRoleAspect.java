package com.worktrack.aspect;

import com.worktrack.annotation.RequireRole;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Aspect
@Component
public class RequireRoleAspect {

    @Before("@annotation(requireRole)")
    public void checkUserRole(RequireRole requireRole) {
        String[] requiredRoles = requireRole.value();
        String msg = requireRole.message();
        boolean loggable = requireRole.loggable();

        if (loggable) {
            System.out.println("Rol kontrolü: " + Arrays.toString(requiredRoles));
        }

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            throw new IllegalStateException("Security context düzgün initialize edilmemiş");
        }

        Set<String> expectedAuthorities = Stream.of(requiredRoles)
                .map(role -> "ROLE_" + role)
                .collect(Collectors.toSet());

        boolean hasRole = auth.getAuthorities().stream()
                .anyMatch(a -> expectedAuthorities.contains(a.getAuthority()));

        if (!hasRole) {
            throw new AccessDeniedException("Bu işlemi yapmak için aşağıdaki rollerden en az birine sahip olmanız gerekir: " + String.join(", ", expectedAuthorities));
        }
    }
}
