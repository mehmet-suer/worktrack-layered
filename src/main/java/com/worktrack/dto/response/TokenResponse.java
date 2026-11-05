package com.worktrack.dto.response;

import com.worktrack.dto.security.GeneratedToken;

public record TokenResponse(
        String token,
        String type,
        long expiresAt
) {
    public static TokenResponse of(GeneratedToken token) {
        return new TokenResponse(
                token.token(),
                token.type().name(),
                token.expiresAt()
        );
    }
}
