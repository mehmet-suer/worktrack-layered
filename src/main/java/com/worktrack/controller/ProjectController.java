package com.worktrack.controller;

import com.worktrack.dto.request.project.CreateProjectRequest;
import com.worktrack.dto.request.validation.Create;
import com.worktrack.dto.response.project.ProjectResponse;
import com.worktrack.service.project.ProjectService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@RequestBody @Validated(Create.class) CreateProjectRequest request) {
        return ResponseEntity
                .status(201)
                .body(projectService.createProject(request));
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> getAllProjects(Pageable pageable) {
        return ResponseEntity.ok(projectService.getAllProjects(pageable));
    }

    @GetMapping("/me")
    public ResponseEntity<List<ProjectResponse>> getAllProjectsForCurrentUser() {
        return ResponseEntity.ok(projectService.getAllProjectsForCurrentUser());
    }

    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @PostAuthorize("returnObject == null || returnObject.ownerUsername == authentication.name")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getProject(Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void>  deleteProject(Long id) {
        return ResponseEntity
                .noContent()
                .build();
    }
}
