package com.worktrack.controller;

import com.worktrack.dto.request.auth.LoginRequest;
import com.worktrack.dto.response.LoginResponse;
import com.worktrack.dto.response.user.UserDto;
import com.worktrack.service.auth.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "User authentication endpoints")
@RestController
@RequestMapping("layered/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @SecurityRequirements
    @PreAuthorize("isAnonymous()")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.authenticate(request.username(), request.password());
        return ResponseEntity.ok(response);
    }


    @GetMapping("/me")
    public ResponseEntity<UserDto> getUserName(@Valid @RequestHeader("Authorization") String authHeader) {
        UserDto userDto = authService.getUserInfoFromToken(authHeader);
        return ResponseEntity.ok(userDto);
    }
}
