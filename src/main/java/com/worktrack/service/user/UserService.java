package com.worktrack.service.user;

import com.worktrack.dto.request.user.RegisterUserRequest;
import com.worktrack.dto.request.user.SearchUserRequest;
import com.worktrack.dto.request.user.UpdateUserRequest;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;

import java.util.List;

public interface UserService {
    UserResponse register(RegisterUserRequest request);

    UserResponse update(Long id, UpdateUserRequest request);

    UserResponse assignRole(Long id, Role role);

    List<UserResponse> findAllByRole(Role role);

    List<UserResponse> findAll();

    void deleteUser(Long id);

    UserResponse findByIdForced(Long id);

    User requireActiveByUsername(String username);

    User requireActiveById(Long id);

    List<UserResponse> search(SearchUserRequest request);
}
