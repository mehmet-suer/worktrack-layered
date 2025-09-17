package com.worktrack.controller;

import com.worktrack.dto.response.DownloadFileResponse;
import com.worktrack.dto.response.project.TaskAttachmentResponse;
import com.worktrack.service.project.TaskAttachmentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Attachments", description = "Task Attachments management endpoints")
@RestController
@RequestMapping("layered/api/v1")
public class TaskAttachmentController {

    private final TaskAttachmentService taskAttachmentService;

    public TaskAttachmentController(TaskAttachmentService taskAttachmentService) {
        this.taskAttachmentService = taskAttachmentService;
    }

    @PostMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<Void> uploadAttachment( @PathVariable("taskId") Long taskId, @RequestParam("file") MultipartFile file) {
        this.taskAttachmentService.uploadAttachment(taskId, file);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/tasks/{taskId}/attachments")
    public ResponseEntity<List<TaskAttachmentResponse>> getAllAttachments(@PathVariable("taskId") Long taskId) {
        return ResponseEntity.ok(this.taskAttachmentService.getAllAttachments(taskId));
    }



    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long attachmentId) {
        DownloadFileResponse response = taskAttachmentService.downloadAttachment(attachmentId);
        return ResponseEntity.ok()
                .contentType(response.contentType())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + response.fileName() + "\"")
                .body(response.file());
    }

    @DeleteMapping("/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long attachmentId) {
        taskAttachmentService.deleteAttachment(attachmentId);
        return ResponseEntity.noContent().build();
    }

}
