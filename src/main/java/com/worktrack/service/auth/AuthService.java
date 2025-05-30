package com.worktrack.service.auth;

import com.worktrack.dto.response.LoginResponse;

public interface AuthService {
    LoginResponse authenticate(String username, String rawPassword);
}
