package com.worktrack.entity.project;

import com.worktrack.entity.base.AuditableBaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "task_attachment")
public class TaskAttachment extends AuditableBaseEntity {

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    @Size(max = 1024)
    private String filePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    public TaskAttachment() {
    }

    public TaskAttachment(String fileName, String filePath, Task task) {
        this.fileName = fileName;
        this.filePath = filePath;
        this.task = task;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }
}
