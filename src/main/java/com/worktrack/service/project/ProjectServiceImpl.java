package com.worktrack.service.project;

import com.worktrack.dto.request.project.CreateProjectRequest;
import com.worktrack.dto.response.project.ProjectResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.entity.project.Project;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.infra.observability.SpanNames;
import com.worktrack.infra.retry.TransientDbRetry;
import com.worktrack.mapper.ProjectResponseMapper;
import com.worktrack.repo.ProjectRepository;
import com.worktrack.security.auth.CurrentUserResolver;
import com.worktrack.service.user.UserService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final CurrentUserResolver currentUserResolver;

    private final ProjectResponseMapper projectResponseMapper;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              UserService userService,
                              CurrentUserResolver currentUserResolver,
                              ProjectResponseMapper projectResponseMapper) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.currentUserResolver = currentUserResolver;
        this.projectResponseMapper = projectResponseMapper;
    }

    @WithSpan(SpanNames.PROJECT_CREATE)
    @Transactional
    @Override
    public ProjectResponse createProject(CreateProjectRequest request) {
        var project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());

        User owner = null;
        if (request.ownerId() != null) {
            owner = userService.requireActiveById(request.ownerId());
            project.setOwner(owner);
        }
        var createdProject = projectRepository.save(project);
        return projectResponseMapper.toDto(createdProject);
    }

    @Override
    @Transactional(readOnly = true)
    @TransientDbRetry
    public List<ProjectResponse> getAllProjectsForCurrentUser() {
        var currentUser = currentUserResolver.getCurrentUserId();
        return projectRepository.findAllActiveByOwnerIdWithOwner(currentUser)
                .stream()
                .map(projectResponseMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @TransientDbRetry
    public Page<ProjectResponse> getAllProjects(Pageable pageable) {
        Page<Project> page = projectRepository.findAllActive(pageable);

        List<ProjectResponse> responses = page.getContent().stream()
                .map(projectResponseMapper::toDto)
                .toList();

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }



    @Override
    @TransientDbRetry
    @Transactional(readOnly = true)
    public Project findByIdForced(Long id) {
        return projectRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project with id " + id + " not found"));
    }
    @Override
    @TransientDbRetry
    @Transactional(readOnly = true)
    public Project findByIdWithOwnerForced(Long id) {
        return projectRepository.findActiveByIdWithOwner(id)
                .orElseThrow(() -> new EntityNotFoundException("Project with id " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    @TransientDbRetry
    public ProjectResponse getProjectById(Long id) {
        Project project = findByIdWithOwnerForced(id);
        return projectResponseMapper.toDto(project);
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        Project project = findByIdForced(id);
        project.setStatus(Status.DELETED);
    }
}
