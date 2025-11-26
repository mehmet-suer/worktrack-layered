package com.worktrack.entity.project;

import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.StatusAwareBaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "task")
public class Task extends StatusAwareBaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false)
    private String title;

    @Size(max = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_status", nullable = false)
    private TaskStatus taskStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_to_user_id")
    private User assignedTo;

    public Task(String title, String description, Project project){
        this.title = title;
        this.description = description;
        this.project = project;
    }

    public Task(String title, String description, Project project, User assignedTo) {
        this.title = title;
        this.description = description;
        this.project = project;
        this.assignedTo = assignedTo;
    }

    public Task() {
    }

    @PrePersist
    protected void onCreateTask() {
        if (this.taskStatus == null) {
            this.taskStatus = TaskStatus.TODO;
        }
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getTaskStatus() {
        return taskStatus;
    }

    public Project getProject() {
        return project;
    }

    public User getAssignedTo() {
        return assignedTo;
    }

    public void assignTo(User assignedTo) {
        this.assignedTo = assignedTo;
    }


}
