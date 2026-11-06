package com.worktrack.service.auth;

import com.worktrack.dto.response.AuthTokensResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.exception.auth.InvalidCredentialsException;
import com.worktrack.infra.observability.SpanNames;
import com.worktrack.security.auth.CurrentUserResolver;
import com.worktrack.service.user.UserService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final AuthenticationTokenLifecycle tokenLifecycle;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserResolver currentUserResolver;

    public AuthService(AuthenticationTokenLifecycle tokenLifecycle,
                       UserService userService,
                       PasswordEncoder passwordEncoder,
                       CurrentUserResolver currentUserResolver) {
        this.tokenLifecycle = tokenLifecycle;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.currentUserResolver = currentUserResolver;
    }

    @WithSpan(SpanNames.AUTH_LOGIN)
    @Transactional
    public AuthTokensResponse authenticate(String username, String password) {
        User user = findUserForAuthentication(username);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        return tokenLifecycle.issueFor(user);
    }

    @WithSpan(SpanNames.AUTH_REFRESH)
    @Transactional
    public AuthTokensResponse refresh(String refreshToken) {
        return tokenLifecycle.rotate(refreshToken);
    }

    @WithSpan(SpanNames.AUTH_LOGOUT)
    @Transactional
    public void logout(String refreshToken) {
        tokenLifecycle.revoke(refreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUserInfo() {
        return userService.findByIdForced(currentUserResolver.getCurrentUserId());
    }

    private User findUserForAuthentication(String username) {
        try {
            return userService.requireActiveByUsername(username);
        } catch (EntityNotFoundException ex) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

}
