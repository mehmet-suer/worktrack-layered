package com.worktrack.mapper;

import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.project.Project;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectResponseMapperTest {

    private final UserResponseMapper userResponseMapper = mock(UserResponseMapper.class);
    private final ProjectResponseMapper projectResponseMapper = new ProjectResponseMapper(userResponseMapper);

    @Test
    void shouldMapOwnerInsideProjectProjection() {
        User owner = UserTestUtils.dummyUserWithId(1L);
        UserResponse ownerResponse = UserTestUtils.dummyUserResponse();
        Project project = new Project("Project", "Description", owner);
        when(userResponseMapper.toDto(owner)).thenReturn(ownerResponse);

        var response = projectResponseMapper.toDto(project);

        assertThat(response.owner()).isSameAs(ownerResponse);
        verify(userResponseMapper).toDto(owner);
    }

    @Test
    void shouldMapProjectWithoutOwner() {
        Project project = new Project("Project", "Description", null);

        var response = projectResponseMapper.toDto(project);

        assertThat(response.owner()).isNull();
        verifyNoInteractions(userResponseMapper);
    }
}
