package com.worktrack.service.auth;

import com.worktrack.dto.security.GeneratedToken;
import com.worktrack.entity.auth.User;

public interface RefreshTokenService {
    GeneratedToken issueRefreshToken(User user);

    RotationResult rotate(String refreshToken);

    void revoke(String refreshToken);

    record RotationResult(User user, GeneratedToken refreshToken) {
    }
}
