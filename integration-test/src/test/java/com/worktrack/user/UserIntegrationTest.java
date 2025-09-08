package com.worktrack.user;


import com.worktrack.base.AbstractWebIntegrationTest;
import com.worktrack.dto.request.auth.UserRegistrationRequest;
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

import static com.worktrack.dto.response.ErrorCode.DUPLICATE_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


public class UserIntegrationTest extends AbstractWebIntegrationTest {

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
            assertTrue(userRepository.findByUsername(request.username()).isPresent());
        }


        @Test
        @DisplayName("should return 400 when registration request is invalid")
        void shouldReturn400WhenRegisterRequestIsInvalid() throws Exception {
            // arrange
            UserRegistrationRequest request = UserTestUtils.dummyInvalidRegistrationRequest();
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
                    .andExpect(jsonPath("$.code").value(DUPLICATE_USER.name()))
                    .andExpect(jsonPath("$.message").value("Email already registered"));
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
                    .andExpect(jsonPath("$.code").value(DUPLICATE_USER.name()))
                    .andExpect(jsonPath("$.message").value("Username already registered"));
            assertThat(userRepository.count()).isEqualTo(before);
        }
    }

    private ResultActions register(UserRegistrationRequest req) throws Exception {
        return mockMvc.perform(post("/layered/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(JsonUtils.asJsonString(req))
                .with(csrf()));
    }

}
