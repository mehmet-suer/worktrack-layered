package com.worktrack.service.project;

import com.worktrack.dto.request.project.CreateProjectRequest;
import com.worktrack.dto.response.project.ProjectResponse;
import com.worktrack.entity.project.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProjectService {
    ProjectResponse createProject(CreateProjectRequest request);

    List<ProjectResponse> getAllProjectsForCurrentUser();

    ProjectResponse getProjectById(Long id);

    void deleteProject(Long id);

    Page<ProjectResponse> getAllProjects(Pageable pageable);

    Project findByIdForced(Long id);

    Project findByIdWithOwnerForced(Long id);
}
