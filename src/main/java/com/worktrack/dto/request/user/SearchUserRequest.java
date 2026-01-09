package com.worktrack.dto.request.user;

import com.worktrack.entity.auth.Role;
import jakarta.validation.constraints.Email;

public record SearchUserRequest(
        @Email
        String email,
        String fullName,
        Role role) {
}
