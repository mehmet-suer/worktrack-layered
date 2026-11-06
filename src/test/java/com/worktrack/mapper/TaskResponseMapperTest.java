package com.worktrack.mapper;

import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.project.Project;
import com.worktrack.entity.project.Task;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TaskResponseMapperTest {

    private final UserResponseMapper userResponseMapper = mock(UserResponseMapper.class);
    private final TaskResponseMapper taskResponseMapper = new TaskResponseMapper(userResponseMapper);

    @Test
    void shouldMapAssigneeInsideTaskProjection() {
        User assignee = UserTestUtils.dummyUserWithId(1L);
        UserResponse assigneeResponse = UserTestUtils.dummyUserResponse();
        Task task = new Task("Task", "Description", new Project(), assignee);
        ReflectionTestUtils.setField(task, "taskStatus", com.worktrack.entity.project.TaskStatus.TODO);
        when(userResponseMapper.toDto(assignee)).thenReturn(assigneeResponse);

        var response = taskResponseMapper.toDto(task);

        assertThat(response.assignedTo()).isSameAs(assigneeResponse);
        verify(userResponseMapper).toDto(assignee);
    }

    @Test
    void shouldMapUnassignedTask() {
        Task task = new Task("Task", "Description", new Project());
        ReflectionTestUtils.setField(task, "taskStatus", com.worktrack.entity.project.TaskStatus.TODO);

        var response = taskResponseMapper.toDto(task);

        assertThat(response.assignedTo()).isNull();
        verifyNoInteractions(userResponseMapper);
    }
}
