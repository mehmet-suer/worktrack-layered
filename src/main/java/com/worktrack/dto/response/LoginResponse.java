package com.worktrack.dto.response;

import com.worktrack.dto.security.GeneratedToken;

public record LoginResponse(
        String token,
        String type,
        long expiresAt,
        String refreshToken,
        String refreshTokenType,
        long refreshExpiresAt
) {
    public static LoginResponse of(GeneratedToken accessToken, GeneratedToken refreshToken) {
        return new LoginResponse(
                accessToken.token(),
                accessToken.type().name(),
                accessToken.expiresAt(),
                refreshToken.token(),
                refreshToken.type().name(),
                refreshToken.expiresAt()
        );
    }
}
