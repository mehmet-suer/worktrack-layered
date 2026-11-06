package com.worktrack.service.auth;

import com.worktrack.dto.response.AuthTokensResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.exception.auth.InvalidCredentialsException;
import com.worktrack.infra.observability.SpanNames;
import com.worktrack.security.auth.CurrentUserResolver;
import com.worktrack.security.jwt.JwtService;
import com.worktrack.service.user.UserService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserResolver currentUserResolver;

    public AuthServiceImpl(JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           UserService userService,
                           PasswordEncoder passwordEncoder,
                           CurrentUserResolver currentUserResolver) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.currentUserResolver = currentUserResolver;
    }

    @WithSpan(SpanNames.AUTH_LOGIN)
    @Transactional
    @Override
    public AuthTokensResponse authenticate(String username, String password) {
        User user = findUserForAuthentication(username);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        var accessToken = jwtService.generateToken(user);
        var refreshToken = refreshTokenService.issueRefreshToken(user);
        return AuthTokensResponse.of(accessToken, refreshToken);
    }

    @WithSpan(SpanNames.AUTH_REFRESH)
    @Transactional
    @Override
    public AuthTokensResponse refresh(String refreshToken) {
        var rotation = refreshTokenService.rotate(refreshToken);
        var accessToken = jwtService.generateToken(rotation.user());
        return AuthTokensResponse.of(accessToken, rotation.refreshToken());
    }

    @WithSpan(SpanNames.AUTH_LOGOUT)
    @Transactional
    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    @Override
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
