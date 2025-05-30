package com.worktrack.service.project;

import com.worktrack.dto.response.DownloadFileResponse;
import com.worktrack.dto.response.project.TaskAttachmentResponse;
import com.worktrack.entity.project.TaskAttachment;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TaskAttachmentService {
    void uploadAttachment(Long taskId, MultipartFile file);
    DownloadFileResponse downloadAttachment(Long attachmentId);
    void deleteAttachment(Long attachmentId);
    List<TaskAttachmentResponse> getAllAttachments(Long taskId);
}
