package com.worktrack.repo;

import com.worktrack.entity.project.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, Long> {
    // Custom query methods can be defined here if needed
    // For example, find by task ID or user ID

    List<TaskAttachment> findAllByTaskId(Long taskId);

}
