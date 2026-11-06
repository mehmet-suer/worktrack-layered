package com.worktrack.security.policy;

import com.worktrack.entity.auth.Role;
import com.worktrack.security.auth.CurrentUserResolver;
import org.springframework.stereotype.Component;

@Component("userPolicy")
public class UserPolicy {
    private final CurrentUserResolver currentUserResolver;

    public UserPolicy(CurrentUserResolver currentUserResolver) {
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Used by @PreAuthorize in UserService.
     */
    public boolean canReadUser(Long userId) {
        return isAdmin() || isLoggedInUser(userId);
    }

    /**
     * Used by @PreAuthorize in UserService.
     */
    public boolean canUpdateUser(Long userId) {
        return isAdmin() || isLoggedInUser(userId);
    }

    /**
     * Used by @PreAuthorize in UserService.
     */
    public boolean canDeleteUser(Long userId) {
        return isAdmin() || isLoggedInUser(userId);
    }

    /**
     * Used by @PreAuthorize in UserService.
     */
    public boolean canSearchUsers() {
        return isAdmin();
    }

    /**
     * Used by @PreAuthorize in UserService.
     */
    public boolean canListUsers() {
        return isAdmin();
    }

    /**
     * Used by @PreAuthorize in com.worktrack.service.user.UserService / findAllByRole
     */
    public boolean canListUsersByRole() {
        return isAdmin();
    }

    /**
     * Used by @PreAuthorize in UserService / assignRole
     */
    public boolean canAssignRole() {
        return isAdmin();
    }

    private boolean isLoggedInUser(Long userId) {
        return currentUserResolver.getCurrentUserId().equals(userId);
    }

    private boolean isAdmin() {
        return currentUserResolver.hasRole(Role.ADMIN);
    }

}
