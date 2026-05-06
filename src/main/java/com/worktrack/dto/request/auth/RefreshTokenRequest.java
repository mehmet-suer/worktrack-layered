package com.worktrack.dto.request.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "refreshToken must not be blank")
        String refreshToken
) {
}
