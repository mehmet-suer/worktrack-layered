package com.worktrack.dto.request.project;

import com.worktrack.entity.project.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import org.springframework.lang.Nullable;

public record CreateTaskRequest(
        @NotBlank
        String title,
        String description,
        TaskStatus status,
        @Nullable
        Long assignedTo
) {
}