package com.worktrack.task;

import com.worktrack.base.AbstractWebIntegrationTest;
import com.worktrack.dto.request.auth.LoginRequest;
import com.worktrack.dto.request.project.CreateProjectRequest;
import com.worktrack.dto.request.project.CreateTaskRequest;
import com.worktrack.dto.request.user.RegisterUserRequest;
import com.worktrack.dto.response.LoginResponse;
import com.worktrack.dto.response.project.ProjectResponse;
import com.worktrack.dto.response.project.TaskResponse;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.repo.ProjectRepository;
import com.worktrack.repo.TaskRepository;
import com.worktrack.repo.user.UserRepository;
import com.worktrack.util.JsonUtils;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TaskIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonUtils jsonUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private TaskRepository taskRepository;

    @BeforeEach
    void setup() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Task flow tests")
    class TaskFlowTests {

        @Test
        @DisplayName("should create task under project")
        void shouldCreateTask() throws Exception {
            AuthContext auth = registerAndLogin(Role.MANAGER);
            ProjectResponse project = createProject(auth, "Project Tasks");

            CreateTaskRequest request = new CreateTaskRequest(
                    "Task A",
                    "First task",
                    null
            );

            mockMvc.perform(post("/layered/api/v1/projects/{projectId}/tasks", project.id())
                            .header("Authorization", "Bearer " + auth.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(jsonUtils.asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Task A"));
        }

        @Test
        @DisplayName("should soft-delete task and exclude from list")
        void shouldSoftDeleteTask() throws Exception {
            AuthContext auth = registerAndLogin(Role.MANAGER);
            ProjectResponse project = createProject(auth, "Project Cleanup");

            CreateTaskRequest request = new CreateTaskRequest(
                    "Task B",
                    "Second task",
                    null
            );

            var createResult = mockMvc.perform(post("/layered/api/v1/projects/{projectId}/tasks", project.id())
                            .header("Authorization", "Bearer " + auth.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(jsonUtils.asJsonString(request)))
                    .andExpect(status().isCreated())
                    .andReturn();

            TaskResponse task = jsonUtils.fromJsonString(
                    createResult.getResponse().getContentAsString(),
                    TaskResponse.class
            );

            mockMvc.perform(delete("/layered/api/v1/projects/{projectId}/tasks/{taskId}", project.id(), task.id())
                            .header("Authorization", "Bearer " + auth.token()))
                    .andExpect(status().isNoContent());

            mockMvc.perform(get("/layered/api/v1/projects/{projectId}/tasks", project.id())
                            .header("Authorization", "Bearer " + auth.token())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    private ProjectResponse createProject(AuthContext auth, String name) throws Exception {
        CreateProjectRequest request = new CreateProjectRequest(
                name,
                "Project for tasks",
                auth.userId()
        );

        var createResult = mockMvc.perform(post("/layered/api/v1/projects")
                        .header("Authorization", "Bearer " + auth.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(jsonUtils.asJsonString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return jsonUtils.fromJsonString(
                createResult.getResponse().getContentAsString(),
                ProjectResponse.class
        );
    }

    private AuthContext registerAndLogin(Role role) throws Exception {
        RegisterUserRequest registerRequest = UserTestUtils.dummyRegistrationRequest();
        register(registerRequest).andExpect(status().isOk());

        User user = userRepository.findByUsernameAndStatusNot(registerRequest.username(), Status.DELETED)
                .orElseThrow();

        user.setRole(role);
        userRepository.save(user);

        var loginResult = login(registerRequest.username(), registerRequest.password())
                .andExpect(status().isOk())
                .andReturn();
        LoginResponse loginResponse = jsonUtils.fromJsonString(
                loginResult.getResponse().getContentAsString(),
                LoginResponse.class
        );

        return new AuthContext(loginResponse.token(), user.getId());
    }

    private ResultActions register(RegisterUserRequest req) throws Exception {
        return mockMvc.perform(post("/layered/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonUtils.asJsonString(req)));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/layered/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonUtils.asJsonString(new LoginRequest(username, password))));
    }

    private record AuthContext(String token, Long userId) {}
}
