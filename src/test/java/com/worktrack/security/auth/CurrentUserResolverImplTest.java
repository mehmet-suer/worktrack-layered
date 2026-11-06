package com.worktrack.security.auth;

import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.auth.AuthenticationException;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class CurrentUserResolverImplTest {

    private final CurrentUserResolver currentUserResolver = new CurrentUserResolverImpl();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldResolveCurrentUserIdAndRoleFromSecurityContext() {
        User user = UserTestUtils.dummyUserWithId(42L);
        var authentication = new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThat(currentUserResolver.getCurrentUserId()).isEqualTo(42L);
        assertThat(currentUserResolver.hasRole(Role.EMPLOYEE)).isTrue();
        assertThat(currentUserResolver.hasRole(Role.ADMIN)).isFalse();
    }

    @Test
    void shouldRejectMissingAuthentication() {
        assertThrowsExactly(
                AuthenticationException.class,
                currentUserResolver::getCurrentUserId
        );
    }
}
