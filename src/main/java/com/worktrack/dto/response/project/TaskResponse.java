package com.worktrack.dto.response.project;

import com.worktrack.entity.project.TaskStatus;

public record TaskResponse(Long id, String title, String description, TaskStatus status) {}

