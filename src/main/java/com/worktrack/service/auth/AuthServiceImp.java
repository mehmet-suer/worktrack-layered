package com.worktrack.service.auth;

import com.worktrack.dto.response.LoginResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.auth.InvalidCredentialsException;
import com.worktrack.security.jwt.JwtService;
import com.worktrack.service.user.UserService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImp implements AuthService {

    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImp(JwtService jwtService, UserService userService, PasswordEncoder passwordEncoder) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse authenticate(String username, String password) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid password");
        }
        var generateToken = jwtService.generateToken(user);
        return new LoginResponse(generateToken.token(), generateToken.type(), generateToken.expiresAt());
    }

    @Override
    public UserResponse getUserInfoFromToken(String authHeader) {
        String token = extractToken(authHeader);
        String username = getUsernameFromToken(token);
        User user = findUser(username);
        validateToken(token, user);
        return userService.toDto(user);
    }


    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidCredentialsException("Invalid or missing Authorization header");
        }
        return authHeader.substring(7);
    }

    private String getUsernameFromToken(String token) {
        try {
            return jwtService.extractUsername(token);
        } catch (JwtException e) {
            throw new InvalidCredentialsException("Failed to extract username from token", e);
        }
    }

    private User findUser(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

    private void validateToken(String token, User user) {
        if (!jwtService.isTokenValid(token, user)) {
            throw new InvalidCredentialsException("Invalid token");
        }
    }

}


