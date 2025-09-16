package com.worktrack.dto.request.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.lang.Nullable;

public record CreateTaskRequest(
        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 255)
        String description,

        @Nullable
        Long assignedTo
) {
}