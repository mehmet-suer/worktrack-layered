package com.worktrack.dto.response.project;

import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.base.Status;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        UserResponse owner,
        Instant createdAt,
        Integer version,
        Status status,
        String createdBy,
        String updatedBy
) {
}

