package com.worktrack.repo;

import com.worktrack.entity.base.Status;
import com.worktrack.entity.project.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p JOIN FETCH p.owner WHERE p.owner.id = :ownerId")
    List<Project> findAllByOwnerIdWithOwner(@Param("ownerId") Long ownerId);

    @EntityGraph(attributePaths = {"owner"})
    Page<Project> findByStatus(Status status, Pageable pageable);

    Optional<Project> findByIdAndStatusNot(Long id, Status status);
}
