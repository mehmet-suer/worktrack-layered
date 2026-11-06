package com.worktrack.mapper;

import com.worktrack.dto.response.project.TaskResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.project.Task;
import org.springframework.stereotype.Component;

@Component
public class TaskResponseMapper {
    private final UserResponseMapper userResponseMapper;

    public TaskResponseMapper(UserResponseMapper userResponseMapper) {
        this.userResponseMapper = userResponseMapper;
    }

    public TaskResponse toDto(Task task) {
        UserResponse assignedTo = task.getAssignedTo() == null
                ? null
                : userResponseMapper.toDto(task.getAssignedTo());

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getTaskStatus().name(),
                assignedTo
        );
    }
}
