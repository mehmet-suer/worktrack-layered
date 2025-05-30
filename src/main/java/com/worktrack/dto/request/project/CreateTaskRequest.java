package com.worktrack.dto.request.project;

import com.worktrack.entity.project.TaskStatus;

public record CreateTaskRequest(String title, String description, TaskStatus status) {}