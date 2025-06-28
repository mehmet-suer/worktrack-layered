package com.worktrack.dto.response.project;

import com.worktrack.dto.response.user.UserDto;
import com.worktrack.entity.project.TaskStatus;

public record TaskResponse(Long id, String title, String description, TaskStatus status, UserDto assignedTo) {}

