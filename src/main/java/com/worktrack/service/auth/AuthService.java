package com.worktrack.service.auth;

import com.worktrack.dto.response.LoginResponse;
import com.worktrack.dto.response.user.UserResponse;

public interface AuthService {
    LoginResponse authenticate(String username, String rawPassword);
    LoginResponse refresh(String refreshToken);
    void logout(String refreshToken);

    UserResponse getCurrentUserInfo();
}
