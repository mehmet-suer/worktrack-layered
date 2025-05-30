package com.worktrack.repo;

import com.worktrack.entity.base.Status;
import com.worktrack.entity.project.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByProjectIdAndStatusNot(Long projectId, Status status);
}
