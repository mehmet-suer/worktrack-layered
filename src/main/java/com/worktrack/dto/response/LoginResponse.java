package com.worktrack.dto.response;

public record LoginResponse(
        String token,
        String type,
        long expiresAt
) { }
