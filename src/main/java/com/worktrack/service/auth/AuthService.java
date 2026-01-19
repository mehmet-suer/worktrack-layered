package com.worktrack.service.auth;

import com.worktrack.dto.response.LoginResponse;
import com.worktrack.dto.response.user.UserResponse;

public interface AuthService {
    LoginResponse authenticate(String username, String rawPassword);

    UserResponse getCurrentUserInfo();
}
