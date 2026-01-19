package com.worktrack.service.auth;

import com.worktrack.dto.response.LoginResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.auth.InvalidCredentialsException;
import com.worktrack.security.auth.AuthenticationFacade;
import com.worktrack.security.jwt.JwtService;
import com.worktrack.service.user.UserService;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final JwtService jwtService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationFacade authenticationFacade;

    public AuthServiceImpl(JwtService jwtService, UserService userService, PasswordEncoder passwordEncoder, AuthenticationFacade authenticationFacade) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationFacade = authenticationFacade;
    }

    public LoginResponse authenticate(String username, String password) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        var generateToken = jwtService.generateToken(user);
        return new LoginResponse(generateToken.token(), generateToken.type(), generateToken.expiresAt());
    }

    @Override
    public UserResponse getCurrentUserInfo() {
        String username = authenticationFacade.getCurrentUsername();
        User user = findUser(username);
        return userService.toDto(user);
    }


    private User findUser(String username) {
        return userService.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));
    }

}
