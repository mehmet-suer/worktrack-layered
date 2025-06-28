package com.worktrack.service.auth;

import com.worktrack.dto.response.LoginResponse;
import com.worktrack.dto.response.AuthUserInfo;
import com.worktrack.dto.response.user.UserDto;

public interface AuthService {
    LoginResponse authenticate(String username, String rawPassword);

    UserDto getUserInfoFromToken(String authHeader);
}
