package com.worktrack.service.project;

import com.worktrack.dto.request.project.CreateProjectRequest;
import com.worktrack.dto.response.project.ProjectResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.entity.project.Project;
import com.worktrack.exception.EntityNotFoundException;
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
public class ProjectServiceImpl implements ProjectService {

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final AuthenticationFacade authenticationFacade;

    private final ProjectResponseMapper projectResponseMapper;

    public ProjectServiceImpl(ProjectRepository projectRepository,
                              UserService userService,
                              AuthenticationFacade authenticationFacade,
                              ProjectResponseMapper projectResponseMapper) {
        this.projectRepository = projectRepository;
        this.userService = userService;
        this.authenticationFacade = authenticationFacade;
        this.projectResponseMapper = projectResponseMapper;
    }

    @Transactional
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
    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjectsForCurrentUser() {
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
    @Transactional(readOnly = true)
    public Page<ProjectResponse> getAllProjects(Pageable pageable) {
        Page<Project> page = projectRepository.findByStatus(Status.ACTIVE, pageable);

        List<ProjectResponse> responses = page.getContent().stream()
                .map(project -> {
                    User owner = project.getOwner();
                    return projectResponseMapper.toDto(project, userService.toDto(owner));
                })
                .toList();

        return new PageImpl<>(responses, pageable, page.getTotalElements());
    }



    @Override
    public Project findByIdForced(Long id) {
        return projectRepository.findByIdAndStatusNot(id, Status.DELETED)
                .orElseThrow(() -> new EntityNotFoundException("Project with id " + id + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        Project project = findByIdForced(id);
        User owner = project.getOwner();
        return projectResponseMapper.toDto(project, userService.toDto(owner));
    }

    @Override
    @Transactional
    public void deleteProject(Long id) {
        Project project = findByIdForced(id);
        project.setStatus(Status.DELETED);
    }

    @Override
    public Optional<Project> findById(Long id) {
        return projectRepository.findById(id);
    }
}
