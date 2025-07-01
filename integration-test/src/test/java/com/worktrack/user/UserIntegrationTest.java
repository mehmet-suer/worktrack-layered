package com.worktrack.user;


import com.worktrack.dto.request.auth.UserRegistrationRequest;
import com.worktrack.repo.user.UserRepository;
import com.worktrack.util.JsonUtils;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static com.worktrack.dto.response.ErrorCode.DUPLICATE_USER;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class UserIntegrationTest {

    @Autowired
    private MockMvc mockMvc;


    @Autowired
    private UserRepository userRepository;


    @BeforeEach
    void setup() {
        userRepository.deleteAll();
    }


    @Nested
    @DisplayName("User Registration Tests")
    class UserRegistrationTests {

        @Test
        @DisplayName("should register user successfully")
        void shouldRegisterUserSuccessfully() throws Exception {
            var request = UserTestUtils.dummyRegistrationRequest();

            mockMvc.perform(post("/layered/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.asJsonString(request))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.username").value(request.username()))
                    .andExpect(jsonPath("$.email").value(request.email()));

            assertTrue(userRepository.findByUsername(request.username()).isPresent());
        }


        @Test
        @DisplayName("should return 400 when registration request is invalid")
        void shouldReturn400WhenRegisterRequestIsInvalid() throws Exception {
            UserRegistrationRequest request = UserTestUtils.dummyInvalidRegistrationRequest();
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/layered/api/v1/users/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(JsonUtils.asJsonString(request))
                    )
                    .andExpect(status().isBadRequest())
                    .andReturn();
        }


        @Test
        @DisplayName("should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() throws Exception {
            var firstUserRequest = UserTestUtils.dummyRegistrationRequest();
            mockMvc.perform(post("/layered/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.asJsonString(firstUserRequest))
                            .with(csrf()))
                    .andExpect(status().isOk());
            var duplicateEmailRequest = UserTestUtils.dummyRegisterRequestWithEmail(firstUserRequest.email());
            mockMvc.perform(post("/layered/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.asJsonString(duplicateEmailRequest))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(DUPLICATE_USER.name()))
                    .andExpect(jsonPath("$.message").value("Email already registered"));
        }

        @Test
        @DisplayName("should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameAlreadyExists() throws Exception {
            var firstUserRequest = UserTestUtils.dummyRegistrationRequest();
            mockMvc.perform(post("/layered/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.asJsonString(firstUserRequest))
                            .with(csrf()))
                    .andExpect(status().isOk());

            var duplicateUsernameRequest = UserTestUtils.dummyRegisterRequestWithUsername(firstUserRequest.username());
            mockMvc.perform(post("/layered/api/v1/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(JsonUtils.asJsonString(duplicateUsernameRequest))
                            .with(csrf()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(DUPLICATE_USER.name()))
                    .andExpect(jsonPath("$.message").value("Username already registered"));
        }
    }
}
