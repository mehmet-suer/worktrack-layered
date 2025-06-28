package com.worktrack.dto.request.project;

import jakarta.validation.constraints.NotNull;

public record AssignTaskRequest(
        @NotNull Long userId
) {}