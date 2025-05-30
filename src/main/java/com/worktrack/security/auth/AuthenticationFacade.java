package com.worktrack.security.auth;

import org.springframework.security.core.Authentication;

public interface AuthenticationFacade {
    Authentication getAuthentication();
    String getCurrentUsername();
    Long getCurrentUserId();
}
