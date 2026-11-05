package com.worktrack.service.auth;

import com.worktrack.dto.response.AuthTokensResponse;
import com.worktrack.dto.response.user.UserResponse;

public interface AuthService {
    AuthTokensResponse authenticate(String username, String rawPassword);
    AuthTokensResponse refresh(String refreshToken);
    void logout(String refreshToken);

    UserResponse getCurrentUserInfo();
}
