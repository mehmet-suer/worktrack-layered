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

    @Query("""
                SELECT p
                FROM Project p
                JOIN FETCH p.owner
                WHERE p.owner.id = :ownerId
                  AND p.status <> :deletedStatus
            """)
    List<Project> findAllByOwnerIdWithOwner(@Param("ownerId") Long ownerId, @Param("deletedStatus") Status deletedStatus);

    @EntityGraph(attributePaths = {"owner"})
    Page<Project> findByStatus(Status status, Pageable pageable);

    Optional<Project> findByIdAndStatusNot(Long id, Status status);


    @Query("""
                SELECT p
                FROM Project p
                LEFT JOIN FETCH p.owner o
                WHERE p.id = :id
                  AND p.status <> :deletedStatus
            """)
    Optional<Project> findByIdWithOwner(@Param("id") Long id, @Param("deletedStatus") Status deletedStatus);

    default List<Project> findAllActiveByOwnerIdWithOwner(Long ownerId) {
        return findAllByOwnerIdWithOwner(ownerId, Status.DELETED);
    }

    default Page<Project> findAllActive(Pageable pageable) {
        return findByStatus(Status.ACTIVE, pageable);
    }

    default Optional<Project> findActiveById(Long id) {
        return findByIdAndStatusNot(id, Status.DELETED);
    }

    default Optional<Project> findActiveByIdWithOwner(Long id) {
        return findByIdWithOwner(id, Status.DELETED);
    }
}
