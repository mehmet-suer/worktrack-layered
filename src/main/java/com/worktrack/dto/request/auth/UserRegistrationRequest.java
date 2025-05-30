package com.worktrack.dto.request.auth;

import com.worktrack.entity.auth.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank
        @Email
        String email,

        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        String fullName,

        @NotNull
        Role role
) {}
