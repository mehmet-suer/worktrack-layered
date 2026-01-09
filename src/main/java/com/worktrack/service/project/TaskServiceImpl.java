package com.worktrack.service.project;

import com.worktrack.common.id.ProjectTaskKey;
import com.worktrack.dto.request.project.AssignTaskRequest;
import com.worktrack.dto.request.project.CreateTaskRequest;
import com.worktrack.dto.response.project.TaskResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.entity.project.Project;
import com.worktrack.entity.project.Task;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.infra.hibernate.HibernateFilterManager;
import com.worktrack.repo.TaskRepository;
import com.worktrack.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final HibernateFilterManager hibernateFilterManager;

    public TaskServiceImpl(TaskRepository taskRepository,
                           ProjectService projectService,
                           UserService userService,

                           HibernateFilterManager hibernateFilterManager) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
        this.userService = userService;

        this.hibernateFilterManager = hibernateFilterManager;
    }

    @Transactional
    public TaskResponse createTask(Long projectId, CreateTaskRequest request) {
        hibernateFilterManager.enableNotDeletedFilter();

        Project project = projectService.findByIdForced(projectId);

        Task task = (request.assignedTo() != null)
                ? createTaskWithAssignedUser(request, project)
                : createTaskWithoutAssignedUser(request, project);

        return toResponse(task);
    }

    private Task createTaskWithAssignedUser(CreateTaskRequest request, Project project) {
        User assignedTo = userService.findEntityByIdForced(request.assignedTo());
        Task task = new Task(request.title(), request.description(), project, assignedTo);
        taskRepository.save(task);
        return task;
    }

    private Task createTaskWithoutAssignedUser(CreateTaskRequest request, Project project) {
        Task task = new Task(request.title(), request.description(), project);
        taskRepository.save(task);
        return task;
    }

    @Transactional
    public TaskResponse assignTask(ProjectTaskKey projectTaskKey, AssignTaskRequest request) {
        hibernateFilterManager.enableNotDeletedFilter();
        Task task = findByIdAndProjectIdForced(projectTaskKey);
        User user = userService.findEntityByIdForced(request.userId());
        task.assignTo(user);

        taskRepository.save(task);
        return toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public Task findByIdAndProjectIdForced(ProjectTaskKey projectTaskKey) {
        return taskRepository.findByIdAndProjectId(projectTaskKey.taskId().value(), projectTaskKey.projectId().value(), Status.DELETED)
                .orElseThrow(() ->
                        new EntityNotFoundException("Task with " + projectTaskKey.taskId().value() +
                                " and ProjectId " + projectTaskKey.projectId().value() + " not found")
                );
    }


    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId) {
        return taskRepository.findAllByProjectIdAndStatusNot(projectId, Status.DELETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteTask(ProjectTaskKey projectTaskKey) {
        Task task = findByIdAndProjectIdForced(projectTaskKey);
        task.setStatus(Status.DELETED);
        taskRepository.save(task);  // NOTE: Due to Hibernate dirty checking, calling save() here is not required.
                                    // The entity will be automatically updated when the transaction is committed.
    }



    private TaskResponse toResponse(Task task) {
        UserResponse userResponse = (task.getAssignedTo() != null)
                ? userService.toDto(task.getAssignedTo())
                : null;

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getTaskStatus(),
                userResponse
        );
    }
}
