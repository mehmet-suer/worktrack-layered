package com.worktrack.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(

        @Size(min = 3, max = 50)
        String username,

        @Email
        String email,

        @Size(min = 6)
        String password,

        String fullName

) {}