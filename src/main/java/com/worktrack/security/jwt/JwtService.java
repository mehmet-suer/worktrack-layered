package com.worktrack.security.jwt;

import com.worktrack.dto.security.GeneratedToken;
import com.worktrack.entity.auth.User;
import io.jsonwebtoken.Claims;

public interface JwtService {
    GeneratedToken generateToken(User user);
    String extractUsername(String token);
    Claims validateToken(String token);
}
