package com.worktrack.dto.response;

import com.worktrack.dto.security.TokenType;

public record LoginResponse(
        String token,
        TokenType type,
        long expiresAt
) { }
