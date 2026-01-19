package com.worktrack.dto.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterUserRequest(

        @NotBlank
        @Size(min = 3, max = 50)
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @NotBlank
        @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters long")
        String password,

        @NotBlank
        @Size(min = 3, max = 100)
        String fullName
) {}
