package com.worktrack.service.project;

import com.worktrack.dto.request.project.CreateProjectRequest;
import com.worktrack.dto.response.project.ProjectResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.entity.project.Project;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.infra.hibernate.HibernateFilterManager;
import com.worktrack.mapper.ProjectResponseMapper;
import com.worktrack.repo.ProjectRepository;
import com.worktrack.security.auth.AuthenticationFacade;
import com.worktrack.service.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(rollbackFor = Exception.class) // Roll back for all exceptions (including checked exceptions); by default, only RuntimeException triggers rollback.
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final AuthenticationFacade authenticationFacade;
    private final HibernateFilterManager hibernateFilterManager;
    private final ProjectResponseMapper projectResponseMapper;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              UserService userService,
                              AuthenticationFacade authenticationFacade,
                              ProjectResponseMapper projectResponseMapper,
                              HibernateFilterManager hibernateFilterManager) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.authenticationFacade = authenticationFacade;
        this.projectResponseMapper = projectResponseMapper;
        this.hibernateFilterManager = hibernateFilterManager;
    }

    @Override
    public ProjectResponse createProject(CreateProjectRequest request) {
        var project = new Project();
        project.setName(request.name());
        project.setDescription(request.description());

        User owner = null;
        if (request.ownerId() != null) {
            owner = userService.findEntityByIdForced(request.ownerId());
            project.setOwner(owner);
        }
        var createdProject = projectRepository.save(project);
        return projectResponseMapper.toDto(createdProject, userService.toDto(owner));
    }

    @Override
    public List<ProjectResponse> getAllProjectsForCurrentUser() {
        hibernateFilterManager.enableNotDeletedFilter();
        var currentUser = authenticationFacade.getCurrentUserId();
        return projectRepository.findAllByOwnerIdWithOwner(currentUser)
                .stream()
                .map(project -> {
                    var owner = project.getOwner(); // NOTE: This must be done inside the service layer.
                                                    // If accessed in the controller, the session will be closed
                                                    // and a LazyInitializationException will be thrown.
                    UserResponse ownerDto = userService.toDto(owner);
                    return projectResponseMapper.toDto(project, ownerDto);
                })
                .toList();
    }

    @Override
    public Page<ProjectResponse> getAllProjects(Pageable pageable) {
        hibernateFilterManager.enableNotDeletedFilter();

        Page<Long> idsPage = projectRepository.findActiveProjectIds(pageable);
        List<Project> projects = projectRepository.findAllByIdInWithOwner(idsPage.getContent());

        List<ProjectResponse> responses = projects.stream()
                .map(project -> {
                    User owner = project.getOwner();
                    return projectResponseMapper.toDto(project, userService.toDto(owner));
                })
                .toList();

        return new PageImpl<>(responses, pageable, idsPage.getTotalElements());
    }



    public Page<ProjectResponse> getAllProjectsV2(Pageable pageable) {
        Page<Project> page = projectRepository.findByStatus(Status.ACTIVE, pageable);
        List<ProjectResponse> responses = page.getContent().stream()
                .map(project -> projectResponseMapper.toDto(project, userService.toDto(project.getOwner())))
                .toList();

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }

    public Project findByIdForced(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Project with id " + id + " not found"));
    }

    @Override
    public ProjectResponse getProjectById(Long id) {
        Project project = findByIdForced(id);
        User owner = project.getOwner();
        return projectResponseMapper.toDto(project, userService.toDto(owner));
    }

    @Override
    public void deleteProject(Long id) {
        Project project = findByIdForced(id);
        project.setStatus(Status.DELETED);
        projectRepository.save(project);
    }

    @Override
    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }
}
