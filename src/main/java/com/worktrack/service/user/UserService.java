package com.worktrack.service.user;

import com.worktrack.dto.request.auth.UserRegistrationRequest;
import com.worktrack.dto.request.auth.UserUpdateRequest;
import com.worktrack.dto.response.user.UserDto;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;

import java.util.List;
import java.util.Optional;

public interface UserService {


    UserDto register(UserRegistrationRequest request);

    UserDto update(Long id, UserUpdateRequest request);

    List<UserDto> findAllByRole(Role role);

    Optional<UserDto> findById(Long id);

    Optional<User> findByUsername(String username);

    Optional<UserDto> findByEmail(String email);

    List<UserDto> findAll();

    void deleteUser(Long id);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    UserDto findByIdForced(Long id);

    User findEntityByIdForced(Long id);

    UserDto toDto(User user);
}
