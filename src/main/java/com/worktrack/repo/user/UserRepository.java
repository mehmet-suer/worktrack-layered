package com.worktrack.repo.user;

import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsernameAndStatusNot(String username, Status status);

    List<User> findByRole(Role role);

    List<User> findByRoleAndStatusNot(Role role, Status status);

    Optional<User> findByIdAndStatusNot(Long id, Status status);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    default Optional<User> findActiveByUsername(String username) {
        return findByUsernameAndStatusNot(username, Status.DELETED);
    }

    default List<User> findAllActiveByRole(Role role) {
        return findByRoleAndStatusNot(role, Status.DELETED);
    }

    default Optional<User> findActiveById(Long id) {
        return findByIdAndStatusNot(id, Status.DELETED);
    }

    List<User> findAllByStatusNot(Status status);

    default List<User> findAllActives() {
        return findAllByStatusNot(Status.DELETED);
    }
}
