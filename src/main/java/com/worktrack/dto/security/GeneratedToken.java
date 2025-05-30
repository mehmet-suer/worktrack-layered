package com.worktrack.dto.security;

public record GeneratedToken(
    String token,
    TokenType type,
    long expiresAt
){}
