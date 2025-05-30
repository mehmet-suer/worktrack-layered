package com.worktrack.mapper;

import com.worktrack.dto.response.user.UserDto;
import com.worktrack.entity.auth.User;
import org.springframework.stereotype.Component;

@Component
public class UserDtoMapper {
    public UserDto toDto(User user) {
        return new UserDto(
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
