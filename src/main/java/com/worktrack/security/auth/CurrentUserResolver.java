package com.worktrack.security.auth;

import com.worktrack.entity.auth.Role;

public interface CurrentUserResolver {
    Long getCurrentUserId();

    boolean hasRole(Role role);
}
