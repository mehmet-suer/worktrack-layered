package com.worktrack.dto.request.user;

import com.worktrack.entity.auth.Role;
import jakarta.validation.constraints.NotNull;

public record AssignRoleRequest(
        @NotNull Role role
) {}
