package com.worktrack.service.user;

import com.worktrack.dto.request.user.RegisterUserRequest;
import com.worktrack.dto.request.user.SearchUserRequest;
import com.worktrack.dto.request.user.UpdateUserRequest;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;

import java.util.List;
import java.util.Optional;

public interface UserService {


    UserResponse register(RegisterUserRequest request);

    UserResponse update(Long id, UpdateUserRequest request);

    List<UserResponse> findAllByRole(Role role);

    Optional<User> findByUsername(String username);

    Optional<UserResponse> findByEmail(String email);

    List<UserResponse> findAll();

    void deleteUser(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    UserResponse findByIdForced(Long id);

    User findEntityByIdForced(Long id);

    UserResponse toDto(User user);

    List<UserResponse> search(SearchUserRequest request);
}
