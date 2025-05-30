package com.worktrack.service.project;

import com.worktrack.dto.request.project.CreateTaskRequest;
import com.worktrack.dto.response.project.TaskResponse;
import com.worktrack.entity.base.Status;
import com.worktrack.entity.project.Project;
import com.worktrack.entity.project.Task;
import com.worktrack.entity.project.TaskStatus;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.repo.TaskRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TaskServiceImpl implements TaskService {
    private final TaskRepository taskRepository;
    private final ProjectService projectService;

    public TaskServiceImpl(TaskRepository taskRepository, ProjectService projectService) {
        this.taskRepository = taskRepository;
        this.projectService = projectService;
    }

    @Transactional
    public TaskResponse createTask(Long projectId, CreateTaskRequest request) {
        Project project = projectService.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found"));

        Task task = new Task(
                request.title(),
                request.description(),
                project
        );
        taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByProject(Long projectId) {
        return taskRepository.findAllByProjectIdAndStatusNot(projectId, Status.DELETED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteTask(Long taskId) {
        Task task = findEntityByIdForced(taskId);
        task.setStatus(Status.DELETED);
        // taskRepository.save(task); // DIKKAT: dirty checking, buna gerek yok cunku save yapmazsan bile delete yapar, transactional annotationu oldugu icin.
    }

    @Override
    public Task findEntityByIdForced(Long taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found"));
    }


    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getTaskStatus()
        );
    }
}
