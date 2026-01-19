package com.worktrack.repo;

import com.worktrack.entity.base.Status;
import com.worktrack.entity.project.Project;
import com.worktrack.entity.project.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findAllByProjectIdAndStatusNot(Long projectId, Status status);

    default List<Task> findAllActivesByProjectId(Long projectId) {
        return findAllByProjectIdAndStatusNot(projectId, Status.DELETED);
    }

    @Query("SELECT t FROM Task t WHERE t.id = :taskId AND t.project.id = :projectId AND t.status <> :deletedStatus")
    Optional<Task> findByIdAndProjectIdAndStatusNot(@Param("taskId") Long taskId, @Param("projectId") Long projectId, @Param("deletedStatus") Status deletedStatus);


    default Optional<Task> findActiveByIdAndProjectId(Long taskId , Long projectId) {
        return findByIdAndProjectIdAndStatusNot(taskId, projectId, Status.DELETED);
    }
}
