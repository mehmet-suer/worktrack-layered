package com.worktrack.controller;

import com.worktrack.dto.request.auth.LoginRequest;
import com.worktrack.dto.response.LoginResponse;
import com.worktrack.service.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println(auth);                         // AnonymousAuthenticationToken
        System.out.println(auth.getPrincipal());          // "anonymousUser"
        System.out.println(auth.getAuthorities());


        LoginResponse response = authService.authenticate(request.username(), request.password());
        return ResponseEntity.ok(response);
    }

}
