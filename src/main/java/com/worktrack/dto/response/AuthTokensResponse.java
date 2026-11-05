package com.worktrack.dto.response;

import com.worktrack.dto.security.GeneratedToken;

public record AuthTokensResponse(
        TokenResponse accessToken,
        TokenResponse refreshToken
) {
    public static AuthTokensResponse of(GeneratedToken accessToken, GeneratedToken refreshToken) {
        return new AuthTokensResponse(
                TokenResponse.of(accessToken),
                TokenResponse.of(refreshToken)
        );
    }
}
