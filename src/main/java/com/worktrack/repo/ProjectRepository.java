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

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOwnerId(Long ownerId);
    @Query("SELECT p FROM Project p JOIN FETCH p.owner WHERE p.owner.id = :ownerId")
    List<Project> findAllByOwnerIdWithOwner(@Param("ownerId") Long ownerId);

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.owner")
    List<Project> findAllWithOwner();


    @Query("SELECT p.id FROM Project p WHERE p.status = 'ACTIVE'")
    Page<Long> findActiveProjectIds(Pageable pageable);

    @Query("SELECT p FROM Project p LEFT JOIN FETCH p.owner WHERE p.id IN :ids")
    List<Project> findAllByIdInWithOwner(@Param("ids") List<Long> ids);

    @EntityGraph(attributePaths = {"owner"})
    Page<Project> findByStatus(Status status, Pageable pageable);
}
