package com.worktrack.user;


import com.worktrack.base.AbstractWebIntegrationTest;
import com.worktrack.dto.request.user.RegisterUserRequest;
import com.worktrack.dto.response.LoginResponse;
import com.worktrack.dto.response.user.UserResponse;
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

import static com.worktrack.dto.response.ErrorCode.INVALID_CREDENTIAL;
import static com.worktrack.dto.response.ErrorCode.DUPLICATE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


public class UserIntegrationTest extends AbstractWebIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    JsonUtils jsonUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @BeforeEach
    void setup() {
        taskRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
    }


    @Nested
    @DisplayName("User Registration Tests")
    class UserRegistrationTests {

        @Test
        @DisplayName("should register user successfully")
        void shouldRegisterUserSuccessfully() throws Exception {
            // arrange
            var countBeforeTest = userRepository.count();
            var request = UserTestUtils.dummyRegistrationRequest();

            // act
            var result = register(request);

            // assert
            result.andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.username").value(request.username()))
                    .andExpect(jsonPath("$.email").value(request.email()));

            assertThat(userRepository.count()).isEqualTo(countBeforeTest + 1);
            assertTrue(userRepository.findByUsernameAndStatusNot(request.username(), Status.DELETED).isPresent());
        }


        @Test
        @DisplayName("should return 400 when registration request is invalid")
        void shouldReturn400WhenRegisterRequestIsInvalid() throws Exception {
            // arrange
            RegisterUserRequest request = UserTestUtils.dummyInvalidRegistrationRequest();
            // act
            var result = register(request);
            // assert
            result.andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }


        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() throws Exception {
            // arrange
            var firstUserRequest = UserTestUtils.dummyRegistrationRequest();
            register(firstUserRequest).andExpect(status().isOk());
            var before = userRepository.count();
            var duplicateEmailRequest = UserTestUtils.dummyRegisterRequestWithEmail(firstUserRequest.email());

            // act
            var result = register(duplicateEmailRequest);

            // assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(DUPLICATE_USER.name()));
            assertThat(userRepository.count()).isEqualTo(before);
        }

        @Test
        @DisplayName("should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameAlreadyExists() throws Exception {
            // arrange
            var firstUserRequest = UserTestUtils.dummyRegistrationRequest();
            register(firstUserRequest).andExpect(status().isOk());
            var before = userRepository.count();
            var duplicateUsernameRequest = UserTestUtils.dummyRegisterRequestWithUsername(firstUserRequest.username());
            // act
            var result = register(duplicateUsernameRequest);

            // assert
            result.andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(DUPLICATE_USER.name()));
            assertThat(userRepository.count()).isEqualTo(before);
        }
    }

    @Nested
    @DisplayName("Auth flow tests")
    class AuthFlowTests {

        @Test
        @DisplayName("should login and access /auth/me")
        void shouldLoginAndAccessMe() throws Exception {
            // arrange
            var request = UserTestUtils.dummyRegistrationRequest();
            register(request).andExpect(status().isOk());

            // act - login
            var loginResult = login(request.username(), request.password())
                    .andExpect(status().isOk())
                    .andReturn();
            String loginBody = loginResult.getResponse().getContentAsString();
            LoginResponse loginResponse = jsonUtils.fromJsonString(loginBody, LoginResponse.class);

            // act - access protected endpoint
            var meResult = mockMvc.perform(get("/layered/api/v1/auth/me")
                            .header("Authorization", "Bearer " + loginResponse.token())
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();

            UserResponse me = jsonUtils.fromJsonString(meResult.getResponse().getContentAsString(), UserResponse.class);

            // assert
            assertThat(me.username()).isEqualTo(request.username());
        }

        @Test
        @DisplayName("should reject login when user is soft-deleted")
        void shouldRejectLoginWhenUserDeleted() throws Exception {
            // arrange
            var request = UserTestUtils.dummyRegistrationRequest();
            register(request).andExpect(status().isOk());
            var user = userRepository.findByUsernameAndStatusNot(request.username(), Status.DELETED).orElseThrow();
            user.setStatus(Status.DELETED);
            userRepository.save(user);

            // act
            login(request.username(), request.password())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value(INVALID_CREDENTIAL.name()));
        }
    }

    private ResultActions register(RegisterUserRequest req) throws Exception {
        return mockMvc.perform(post("/layered/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonUtils.asJsonString(req))
                .with(csrf()));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mockMvc.perform(post("/layered/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(jsonUtils.asJsonString(new com.worktrack.dto.request.auth.LoginRequest(username, password))));
    }

}
