package com.worktrack.dto.response.user;


public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String role,
        String createdBy,
        String updatedBy
) {
    public UserResponse(Long id, String username, String fullName) {
        this(id, username, null, fullName, null, null, null);
    }

}
