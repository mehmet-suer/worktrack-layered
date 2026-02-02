package com.worktrack.security;

import com.worktrack.base.AbstractWebIntegrationTest;
import com.worktrack.dto.request.auth.LoginRequest;
import com.worktrack.dto.request.project.CreateProjectRequest;
import com.worktrack.dto.request.user.RegisterUserRequest;
import com.worktrack.dto.response.LoginResponse;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.repo.ProjectRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonUtils jsonUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void setup() {
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Nested
    @DisplayName("Authentication & authorization tests")
    class AuthzTests {

        @Test
        @DisplayName("should reject protected endpoint without token")
        void shouldRejectWithoutToken() throws Exception {
            mockMvc.perform(get("/layered/api/v1/projects")
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("should forbid project creation for EMPLOYEE")
        void shouldForbidProjectCreationForEmployee() throws Exception {
            AuthContext auth = registerAndLogin(Role.EMPLOYEE);

            CreateProjectRequest request = new CreateProjectRequest(
                    "Project Sec",
                    "Security check",
                    auth.userId()
            );

            mockMvc.perform(post("/layered/api/v1/projects")
                            .header("Authorization", "Bearer " + auth.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(jsonUtils.asJsonString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("should allow project creation for MANAGER")
        void shouldAllowProjectCreationForManager() throws Exception {
            AuthContext auth = registerAndLogin(Role.MANAGER);

            CreateProjectRequest request = new CreateProjectRequest(
                    "Project Allowed",
                    "Manager role",
                    auth.userId()
            );

            mockMvc.perform(post("/layered/api/v1/projects")
                            .header("Authorization", "Bearer " + auth.token())
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON)
                            .content(jsonUtils.asJsonString(request)))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("should forbid users list for non-admin")
        void shouldForbidUserListForNonAdmin() throws Exception {
            AuthContext auth = registerAndLogin(Role.EMPLOYEE);

            mockMvc.perform(get("/layered/api/v1/users")
                            .header("Authorization", "Bearer " + auth.token())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden());
        }
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
