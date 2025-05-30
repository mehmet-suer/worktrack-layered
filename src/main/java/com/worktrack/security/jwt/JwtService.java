package com.worktrack.security.jwt;

import com.worktrack.dto.security.GeneratedToken;
import com.worktrack.entity.auth.User;

public interface JwtService {
    GeneratedToken generateToken(User user);
    boolean isTokenValid(String token, User user);
    String extractUsername(String token);
}
