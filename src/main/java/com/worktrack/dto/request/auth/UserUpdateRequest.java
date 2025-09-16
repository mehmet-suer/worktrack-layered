package com.worktrack.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @Size(min = 3, max = 50)
        String username,

        @Email
        @Size(max = 100, message = "Email must be at most 100 characters")
        String email,

        @Size(min = 8, max = 50)
        String password,


        @Size(min = 3, max = 100)
        String fullName

) {}