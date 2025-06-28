package com.worktrack.controller;

import com.worktrack.dto.request.auth.LoginRequest;
import com.worktrack.dto.response.LoginResponse;
import com.worktrack.dto.response.AuthUserInfo;
import com.worktrack.dto.response.user.UserDto;
import com.worktrack.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PreAuthorize("isAnonymous()")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.authenticate(request.username(), request.password());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/me")
    public ResponseEntity<UserDto> getUserName(@RequestHeader("Authorization") String authHeader) {
        UserDto userDto = authService.getUserInfoFromToken(authHeader);
        return ResponseEntity.ok(userDto);
    }
}
