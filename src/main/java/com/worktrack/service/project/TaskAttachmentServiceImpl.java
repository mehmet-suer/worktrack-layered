package com.worktrack.service.project;

import com.worktrack.dto.response.DownloadFileResponse;
import com.worktrack.dto.response.project.TaskAttachmentResponse;
import com.worktrack.entity.project.Task;
import com.worktrack.entity.project.TaskAttachment;
import com.worktrack.repo.TaskAttachmentRepository;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
public class TaskAttachmentServiceImpl implements TaskAttachmentService {

    private final TaskAttachmentRepository attachmentRepository;
    private final TaskService taskService;
    private final FileStorageService fileStorageService;

    public TaskAttachmentServiceImpl(TaskAttachmentRepository attachmentRepository,
                                 TaskService taskService,
                                 FileStorageService fileStorageService) {
        this.attachmentRepository = attachmentRepository;
        this.taskService = taskService;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public void uploadAttachment(Long taskId, MultipartFile file) {
        Task task = taskService.findEntityByIdForced(taskId);
        String storedPath = fileStorageService.storeFile(file);

        TaskAttachment attachment = new TaskAttachment();
        attachment.setTask(task);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFilePath(storedPath);
        attachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public DownloadFileResponse downloadAttachment(Long attachmentId) {
        TaskAttachment attachment = getAttachmentForced(attachmentId);
        Resource resource = fileStorageService.loadFile(attachment.getFilePath());
        MediaType contentType = detectContentType(attachment.getFilePath());

        return new DownloadFileResponse(resource, attachment.getFileName(), contentType);
    }

    @Transactional(readOnly = true)
    public List<TaskAttachmentResponse> getAllAttachments(Long taskId) {
        return attachmentRepository.findAllByTaskId(taskId).stream().map(attachment -> new TaskAttachmentResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFilePath()
        )).toList();
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        TaskAttachment attachment = getAttachmentForced(attachmentId);
        fileStorageService.deleteFile(attachment.getFilePath());
        attachmentRepository.delete(attachment);
    }

    private TaskAttachment getAttachmentForced(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
    }

    private MediaType detectContentType(String path) {
        try {
            String type = Files.probeContentType(Paths.get(path));
            return (type != null) ? MediaType.parseMediaType(type) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IOException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
