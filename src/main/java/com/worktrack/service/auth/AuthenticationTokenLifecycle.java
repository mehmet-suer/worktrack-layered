package com.worktrack.service.auth;

import com.worktrack.dto.response.AuthTokensResponse;
import com.worktrack.dto.security.GeneratedToken;
import com.worktrack.entity.auth.User;
import com.worktrack.security.jwt.JwtService;
import org.springframework.stereotype.Component;

@Component
final class AuthenticationTokenLifecycle {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    AuthenticationTokenLifecycle(JwtService jwtService,
                                 RefreshTokenService refreshTokenService) {
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    AuthTokensResponse issueFor(User user) {
        GeneratedToken accessToken = issueAccessToken(user);
        GeneratedToken refreshToken = refreshTokenService.issueRefreshToken(user);
        return AuthTokensResponse.of(accessToken, refreshToken);
    }

    AuthTokensResponse rotate(String refreshToken) {
        var rotation = refreshTokenService.rotate(refreshToken);
        GeneratedToken accessToken = issueAccessToken(rotation.user());
        return AuthTokensResponse.of(accessToken, rotation.refreshToken());
    }

    void revoke(String refreshToken) {
        refreshTokenService.revoke(refreshToken);
    }

    private GeneratedToken issueAccessToken(User user) {
        return jwtService.issueAccessToken(user.getUsername(), user.getRole());
    }
}
