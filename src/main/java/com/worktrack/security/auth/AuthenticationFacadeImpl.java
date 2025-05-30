package com.worktrack.security.auth;

import com.worktrack.entity.auth.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFacadeImpl implements AuthenticationFacade{
    @Override
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public String getCurrentUsername() {
        return getAuthentication().getName();
    }

    @Override
    public Long getCurrentUserId() {
        Object principal = getAuthentication().getPrincipal();
        if (principal instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("User id bulunamadı");
    }
}
