package com.worktrack.controller;


import com.worktrack.config.JsonTestConfig;
import com.worktrack.dto.request.user.RegisterUserRequest;
import com.worktrack.dto.request.user.UpdateUserRequest;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.security.jwt.JwtAuthenticationFilter;
import com.worktrack.security.jwt.JwtService;
import com.worktrack.service.user.UserService;
import com.worktrack.util.JsonUtils;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JsonTestConfig.class)
public class UserControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(UserControllerTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    JsonUtils jsonUtils;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("register() endpoint")
    class RegisterTests {

        @Test
        @DisplayName("should register user successfully")
        void shouldRegisterUserSuccessfully() throws Exception {
            RegisterUserRequest request = UserTestUtils.dummyRegistrationRequest();
            UserResponse expectedUserResponse = UserTestUtils.toDtoFrom(request, 1L);

            when(userService.register(any(RegisterUserRequest.class))).thenReturn(expectedUserResponse);
            var mvcResult = mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/layered/api/v1/users/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonUtils.asJsonString(request))
                    )
                    .andExpect(status().isOk())
                    .andReturn();
            String responseBody = mvcResult.getResponse().getContentAsString();
            UserResponse actual = jsonUtils.fromJsonString(responseBody, UserResponse.class);

            assertEquals(expectedUserResponse, actual);
            verify(userService).register(any(RegisterUserRequest.class));
            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("should return 400 when registration request is invalid")
        void shouldReturn400WhenRegisterRequestIsInvalid() throws Exception {
            RegisterUserRequest request = UserTestUtils.dummyInvalidRegistrationRequest();

            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/layered/api/v1/users/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonUtils.asJsonString(request))
                    )
                    .andExpect(status().isBadRequest())
                    .andReturn();
            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("update() endpoint")
    class UpdateTests {

        @Test
        @DisplayName("should update user successfully")
        void shouldUpdateUserSuccessfully() throws Exception {
            UpdateUserRequest request = UserTestUtils.dummyUpdateRequest();
            var userId = 1L;
            UserResponse expectedUserResponse = UserTestUtils.toDtoFrom(request, userId);

            when(userService.update(eq(userId), any(UpdateUserRequest.class))).thenReturn(expectedUserResponse);
            var mvcResult = mockMvc.perform(
                            MockMvcRequestBuilders
                                    .put("/layered/api/v1/users/{id}", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonUtils.asJsonString(request))
                    )
                    .andExpect(status().isOk())
                    .andReturn();
            String responseBody = mvcResult.getResponse().getContentAsString();
            UserResponse actual = jsonUtils.fromJsonString(responseBody, UserResponse.class);

            assertEquals(expectedUserResponse, actual);
            verify(userService).update(eq(userId), any(UpdateUserRequest.class));
            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("should return 404 when user to update does not exist")
        void shouldReturn404WhenUserToUpdateDoesNotExist() throws Exception {
            UpdateUserRequest request = UserTestUtils.dummyUpdateRequest();
            var userId = 1L;

            when(userService.update(eq(userId), any(UpdateUserRequest.class))).thenThrow(new EntityNotFoundException("User not found"));

            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .put("/layered/api/v1/users/{id}", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonUtils.asJsonString(request))
                    )
                    .andExpect(status().isNotFound());

            verify(userService).update(eq(userId), any(UpdateUserRequest.class));
            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("should return 400 when update request is invalid")
        void shouldReturn400WhenUpdateRequestIsInvalid() throws Exception {
            UpdateUserRequest request = new UpdateUserRequest("", "invalidEmail", "short", "");

            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .put("/layered/api/v1/users/{id}", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(jsonUtils.asJsonString(request))
                    )
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("getAll() endpoint")
    class GetAllTests {
        @Test
        @DisplayName("should return all users")
        void shouldReturnAllUsers() throws Exception {
            var userId = 1L;
            UserResponse userResponse = UserTestUtils.toDtoFrom(UserTestUtils.dummyRegistrationRequest(), userId);
            when(userService.findAll()).thenReturn(List.of(userResponse));

            var mvcResult = mockMvc.perform(
                            MockMvcRequestBuilders
                                    .get("/layered/api/v1/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andReturn();
            String responseBody = mvcResult.getResponse().getContentAsString();
            List<UserResponse> actual = jsonUtils.fromJsonArrayString(responseBody, UserResponse.class);

            assertEquals(1, actual.size());
            assertEquals(userResponse, actual.getFirst());
            verify(userService).findAll();
            verifyNoMoreInteractions(userService);
        }
    }

    @Nested
    @DisplayName("getById() endpoint")
    class GetByIdTests {

        @Test
        @DisplayName("should return user by ID")
        void shouldReturnUserById() throws Exception {
            var userId = 1L;
            UserResponse userResponse = UserTestUtils.toDtoFrom(UserTestUtils.dummyRegistrationRequest(), userId);
            when(userService.findByIdForced(eq(userId))).thenReturn(userResponse);

            var mvcResult = mockMvc.perform(
                            MockMvcRequestBuilders
                                    .get("/layered/api/v1/users/{id}", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andReturn();
            String responseBody = mvcResult.getResponse().getContentAsString();
            UserResponse actual = jsonUtils.fromJsonString(responseBody, UserResponse.class);

            assertEquals(userResponse, actual);
            verify(userService).findByIdForced(userId);
            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("should return 404 when user to find by id does not exist")
        void shouldReturn404WhenUserToFindByIdNotExists() throws Exception {
            var userId = 1L;
            when(userService.findByIdForced(eq(userId))).thenThrow(new EntityNotFoundException("User not Found"));

            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .get("/layered/api/v1/users/{id}", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isNotFound());
            verify(userService).findByIdForced(userId);
        }
    }

    @Nested
    @DisplayName("delete() endpoint")
    class DeleteUserTests {

        @Test
        void shouldDeleteUserSuccessfully() throws Exception {
            var userId = 1L;
            doNothing().when(userService).deleteUser(eq(userId));
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .delete("/layered/api/v1/users/{id}", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isNoContent());

            verify(userService).deleteUser(eq(userId));
            verifyNoMoreInteractions(userService);
        }

        @Test
        void shouldReturn404WhenUserToDeleteNotExists() throws Exception {
            var unknownUserId = 1L;
            doThrow(new EntityNotFoundException("User not found")).when(userService).deleteUser(eq(unknownUserId));
            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .delete("/layered/api/v1/users/{id}", unknownUserId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isNotFound());

            verify(userService).deleteUser(eq(unknownUserId));
            verifyNoMoreInteractions(userService);
        }

    }
}
