package com.worktrack.security.auth;

import org.springframework.security.core.Authentication;
import com.worktrack.entity.auth.Role;

public interface AuthenticationFacade {
    Authentication getAuthentication();
    String getCurrentUsername();
    Long getCurrentUserId();
    boolean hasRole(Role role);
}
