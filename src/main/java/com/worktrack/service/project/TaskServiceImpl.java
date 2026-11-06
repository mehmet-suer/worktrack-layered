package com.worktrack.service.project;

import com.worktrack.common.id.ProjectTaskKey;
import com.worktrack.dto.request.project.AssignTaskRequest;
import com.worktrack.dto.request.project.CreateTaskRequest;
import com.worktrack.dto.response.project.TaskResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.entity.project.Project;
import com.worktrack.entity.project.Task;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.infra.observability.SpanNames;
import com.worktrack.infra.retry.TransientDbRetry;
import com.worktrack.mapper.TaskResponseMapper;
import com.worktrack.repo.TaskRepository;
import com.worktrack.service.user.UserService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final ProjectService projectService;
    private final UserService userService;
    private final TaskResponseMapper taskResponseMapper;

    public TaskServiceImpl(TaskRepository taskRepository,
                           ProjectService projectService,
                           UserService userService,
                           TaskResponseMapper taskResponseMapper) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
        this.userService = userService;
        this.taskResponseMapper = taskResponseMapper;
    }

    @WithSpan(SpanNames.TASK_CREATE)
    @Transactional
    public TaskResponse createTask(Long projectId, CreateTaskRequest request) {

        Project project = projectService.findByIdForced(projectId);

        Task task = (request.assignedTo() != null)
                ? createTaskWithAssignedUser(request, project)
                : createTaskWithoutAssignedUser(request, project);

        return taskResponseMapper.toDto(task);
    }

    private Task createTaskWithAssignedUser(CreateTaskRequest request, Project project) {
        User assignedTo = userService.requireActiveById(request.assignedTo());
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
        Task task = findByIdAndProjectIdForced(projectTaskKey);
        User user = userService.requireActiveById(request.userId());
        task.assignTo(user);

        taskRepository.save(task);
        return taskResponseMapper.toDto(task);
    }

    @Override
    @Transactional(readOnly = true)
    @TransientDbRetry
    public Task findByIdAndProjectIdForced(ProjectTaskKey projectTaskKey) {
        return taskRepository.findByIdAndProjectIdAndStatusNot(projectTaskKey.taskId().value(), projectTaskKey.projectId().value(), Status.DELETED)
                .orElseThrow(() ->
                        new EntityNotFoundException("Task with " + projectTaskKey.taskId().value() +
                                " and ProjectId " + projectTaskKey.projectId().value() + " not found")
                );
    }


    @Transactional(readOnly = true)
    @TransientDbRetry
    public List<TaskResponse> getTasksByProject(Long projectId) {
        return taskRepository.findAllActivesByProjectId(projectId)
                .stream()
                .map(taskResponseMapper::toDto)
                .toList();
    }

    @Transactional
    public void deleteTask(ProjectTaskKey projectTaskKey) {
        Task task = findByIdAndProjectIdForced(projectTaskKey);
        task.setStatus(Status.DELETED);
        taskRepository.save(task);  // NOTE: Due to Hibernate dirty checking, calling save() here is not required.
                                    // The entity will be automatically updated when the transaction is committed.
    }

}
