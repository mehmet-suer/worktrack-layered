package com.worktrack.dto.request.project;

import com.worktrack.dto.request.validation.Create;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank(groups = Create.class)
        @Size(max = 255)
        String name,

        @Size(max = 255)
        String description,

        Long ownerId
) { }
