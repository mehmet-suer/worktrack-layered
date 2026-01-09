package com.worktrack.service.project;

import com.worktrack.common.id.ProjectTaskKey;
import com.worktrack.dto.request.project.AssignTaskRequest;
import com.worktrack.dto.request.project.CreateTaskRequest;
import com.worktrack.dto.response.project.TaskResponse;
import com.worktrack.entity.project.Task;

import java.util.List;

public interface TaskService{
    TaskResponse createTask(Long projectId, CreateTaskRequest request);
    List<TaskResponse> getTasksByProject(Long projectId);
    void deleteTask(ProjectTaskKey projectTaskKey);
    Task findByIdAndProjectIdForced(ProjectTaskKey projectTaskKey);
    TaskResponse assignTask(ProjectTaskKey projectTaskKey, AssignTaskRequest request);
}
