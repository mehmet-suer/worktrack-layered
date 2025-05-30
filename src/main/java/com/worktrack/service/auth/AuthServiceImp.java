package com.worktrack.service.auth;

import com.worktrack.dto.response.LoginResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.auth.InvalidCredentialsException;
import com.worktrack.security.jwt.JwtService;
import com.worktrack.service.user.UserService;
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
}


