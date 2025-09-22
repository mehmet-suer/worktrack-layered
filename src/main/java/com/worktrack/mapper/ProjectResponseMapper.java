package com.worktrack.mapper;

import com.worktrack.dto.response.project.ProjectResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.project.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectResponseMapper {
    public ProjectResponse toDto(Project project, UserResponse owner) {
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

    public ProjectResponse toDto(Project project) {
        return toDto(project, null);
    }
}
