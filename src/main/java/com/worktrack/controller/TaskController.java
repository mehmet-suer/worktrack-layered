package com.worktrack.controller;

import com.worktrack.common.id.ProjectId;
import com.worktrack.common.id.ProjectTaskKey;
import com.worktrack.common.id.TaskId;
import com.worktrack.dto.request.project.AssignTaskRequest;
import com.worktrack.dto.request.project.CreateTaskRequest;
import com.worktrack.dto.response.project.TaskResponse;
import com.worktrack.service.project.TaskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tasks", description = "Task management endpoints")
@RestController
@RequestMapping("layered/api/v1/projects/{projectId}/tasks")
public class TaskController {
    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @PathVariable("projectId") Long projectId,
            @Valid @RequestBody CreateTaskRequest request) {
        TaskResponse response = taskService.createTask(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{taskId}/assign")
    public ResponseEntity<TaskResponse> assignTask(
            @PathVariable("projectId") Long projectId,
            @PathVariable("taskId") Long taskId,
            @Valid @RequestBody AssignTaskRequest request) {
        var projectTaskKey = new ProjectTaskKey( new ProjectId(projectId), new TaskId(taskId));
        TaskResponse response = taskService.assignTask(projectTaskKey, request);
        return ResponseEntity.ok(response);
    }


    @GetMapping
    public ResponseEntity<List<TaskResponse>> getTasks(@PathVariable("projectId") Long projectId) {
        return ResponseEntity.ok(taskService.getTasksByProject(projectId));
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<Void> deleteTask(@PathVariable("projectId") Long projectId, @PathVariable("taskId") Long taskId) {
        var projectTaskKey = new ProjectTaskKey( new ProjectId(projectId), new TaskId(taskId));
        taskService.deleteTask(projectTaskKey);
        return ResponseEntity.noContent().build();
    }

}
