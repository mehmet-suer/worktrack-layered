package com.worktrack.dto.response.project;

import com.worktrack.dto.response.user.UserDto;
import com.worktrack.entity.base.Status;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        String name,
        String description,
        UserDto owner,
        Instant createdAt,
        Integer version,
        Status status,
        String createdBy,
        String updatedBy
) {
}

