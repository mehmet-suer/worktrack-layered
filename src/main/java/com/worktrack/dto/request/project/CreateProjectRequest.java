package com.worktrack.dto.request.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(

        @NotBlank()
        @Size(max = 255)
        String name,

        @Size(max = 255)
        String description,

        Long ownerId
) { }
