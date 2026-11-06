package com.worktrack.mapper;

import com.worktrack.dto.response.project.ProjectResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.project.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectResponseMapper {
    private final UserResponseMapper userResponseMapper;

    public ProjectResponseMapper(UserResponseMapper userResponseMapper) {
        this.userResponseMapper = userResponseMapper;
    }

    public ProjectResponse toDto(Project project) {
        UserResponse owner = project.getOwner() == null
                ? null
                : userResponseMapper.toDto(project.getOwner());

        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getDescription(),
                owner,
                project.getCreatedAt(),
                project.getVersion(),
                project.getStatus(),
                project.getCreatedBy(),
                project.getUpdatedBy()
        );
    }
}
