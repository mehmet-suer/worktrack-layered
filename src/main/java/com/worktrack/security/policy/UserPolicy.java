package com.worktrack.security.policy;

import com.worktrack.entity.auth.Role;
import com.worktrack.security.auth.AuthenticationFacade;
import org.springframework.stereotype.Component;

@Component("userPolicy")
public class UserPolicy {
    private final AuthenticationFacade authenticationFacade;

    public UserPolicy(AuthenticationFacade auth) {
        this.authenticationFacade = auth;
    }

    /**
     * Used by @PreAuthorize in UserServiceImpl.
     */
    public boolean canReadUser(Long userId) {
        return isAdmin() || isLoggedInUser(userId);
    }

    /**
     * Used by @PreAuthorize in UserServiceImpl.
     */
    public boolean canUpdateUser(Long userId) {
        return isAdmin() || isLoggedInUser(userId);
    }

    /**
     * Used by @PreAuthorize in UserServiceImpl.
     */
    public boolean canDeleteUser(Long userId) {
        return isAdmin() || isLoggedInUser(userId);
    }

    /**
     * Used by @PreAuthorize in UserServiceImpl.
     */
    public boolean canSearchUsers() {
        return isAdmin();
    }

    /**
     * Used by @PreAuthorize in UserServiceImpl.
     */
    public boolean canListUsers() {
        return isAdmin();
    }

    /**
     * Used by @PreAuthorize in com.worktrack.service.user.UserServiceImpl / findAllByRole
     */
    public boolean canListUsersByRole() {
        return isAdmin();
    }

    private boolean isLoggedInUser(Long userId) {
        return authenticationFacade.getCurrentUserId().equals(userId);
    }

    private boolean isAdmin() {
        return authenticationFacade.hasRole(Role.ADMIN);
    }

}
