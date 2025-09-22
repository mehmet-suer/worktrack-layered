package com.worktrack.mapper;

import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import org.springframework.stereotype.Component;

@Component
public class UserResponseMapper {
    public UserResponse toDto(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getCreatedBy(),
                user.getUpdatedBy()
        );
    }
}
