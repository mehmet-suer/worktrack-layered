package com.worktrack.dto.request.auth;

import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Size(min = 3, max = 50)
        String username,
        @Size(min = 8, max = 50)
        String password) { }