package com.worktrack.service.auth;

import com.worktrack.dto.response.AuthTokensResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.auth.InvalidCredentialsException;
import com.worktrack.infra.observability.SpanNames;
import com.worktrack.security.auth.AuthenticationFacade;
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
    private final AuthenticationFacade authenticationFacade;

    public AuthServiceImpl(JwtService jwtService,
                           RefreshTokenService refreshTokenService,
                           UserService userService,
                           PasswordEncoder passwordEncoder,
                           AuthenticationFacade authenticationFacade) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationFacade = authenticationFacade;
    }

    @WithSpan(SpanNames.AUTH_LOGIN)
    @Transactional
    @Override
    public AuthTokensResponse authenticate(String username, String password) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

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
        String username = authenticationFacade.getCurrentUsername();
        User user = findUser(username);
        return userService.toDto(user);
    }


    private User findUser(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

}
