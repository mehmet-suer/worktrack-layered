package com.worktrack.repo.user.specification;

import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.auth.User_;
import com.worktrack.entity.base.Status;
import com.worktrack.repo.specification.Spec;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;

public class UserSpecifications {

    public static Specification<User> usernameContains(String username) {
        return Spec.containsRaw(User_.username, username);
    }

    public static Specification<User> fullNameContains(String fullName) {
        return Spec.containsRaw(User_.fullName, fullName);
    }


    public static Specification<User> hasRole(Role role) {
        return Spec.eq(User_.role, role);
    }

    public static Specification<User> hasRoles(Role ...roles) {
        return Spec.in(User_.role, Arrays.stream(roles).toList());
    }


    public static Specification<User> hasEmail(String email) {
        return Spec.eq(User_.email, email);
    }

    public static Specification<User> hasStatus(Status status) {
        return Spec.eq(User_.status, status);
    }
}
