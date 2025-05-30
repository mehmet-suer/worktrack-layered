package com.worktrack.repo.user.specification;

import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecifications {

     public static Specification<User> usernameContains(String username) {
        return (root, query, cb) ->
                username == null ? null : cb.like(cb.lower(root.get("username")), "%" + username.toLowerCase() + "%");
    }


    private static Specification<User> hasProperty(String field, Object value) {
        return (root, query, cb) ->
                value == null ? null : cb.equal(root.get(field), value);
    }

    public static Specification<User> hasRole(Role role) {
        return hasProperty("role", role);
    }

    public static Specification<User> hasStatus(Status status) {
        return hasProperty("status", status);
    }
}
