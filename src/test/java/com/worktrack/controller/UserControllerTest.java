package com.worktrack.controller;


import com.worktrack.dto.request.auth.UserRegistrationRequest;
import com.worktrack.dto.request.auth.UserUpdateRequest;
import com.worktrack.dto.response.user.UserDto;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {
    private static final Logger logger = LoggerFactory.getLogger(UserControllerTest.class);

    @Autowired
    private MockMvc mockMvc;


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
            UserRegistrationRequest request = UserTestUtils.dummyRegistrationRequest();
            UserDto expectedUserDto = UserTestUtils.toDtoFrom(request, 1L);

            when(userService.register(any(UserRegistrationRequest.class))).thenReturn(expectedUserDto);
            var mvcResult = mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/api/users/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(JsonUtils.asJsonString(request))
                    )
                    .andExpect(status().isOk())
                    .andReturn();
            String responseBody = mvcResult.getResponse().getContentAsString();
            UserDto actual = JsonUtils.fromJsonString(responseBody, UserDto.class);

            assertEquals(expectedUserDto, actual);
            verify(userService).register(any(UserRegistrationRequest.class));
            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("should return 400 when registration request is invalid")
        void shouldReturn400WhenRegisterRequestIsInvalid() throws Exception {
            UserRegistrationRequest request = UserTestUtils.dummyInvalidRegistrationRequest();

            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .post("/api/users/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(JsonUtils.asJsonString(request))
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
            UserUpdateRequest request = UserTestUtils.dummyUpdateRequest();
            var userId = 1L;
            UserDto expectedUserDto = UserTestUtils.toDtoFrom(request, userId);

            when(userService.update(eq(userId), any(UserUpdateRequest.class))).thenReturn(expectedUserDto);
            var mvcResult = mockMvc.perform(
                            MockMvcRequestBuilders
                                    .put("/api/users/{id}", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(JsonUtils.asJsonString(request))
                    )
                    .andExpect(status().isOk())
                    .andReturn();
            String responseBody = mvcResult.getResponse().getContentAsString();
            UserDto actual = JsonUtils.fromJsonString(responseBody, UserDto.class);

            assertEquals(expectedUserDto, actual);
            verify(userService).update(eq(userId), any(UserUpdateRequest.class));
            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("should return 404 when user to update does not exist")
        void shouldReturn404WhenUserToUpdateDoesNotExist() throws Exception {
            UserUpdateRequest request = UserTestUtils.dummyUpdateRequest();
            var userId = 1L;

            when(userService.update(eq(userId), any(UserUpdateRequest.class))).thenThrow(new EntityNotFoundException("User not found"));

            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .put("/api/users/{id}", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(JsonUtils.asJsonString(request))
                    )
                    .andExpect(status().isNotFound());

            verify(userService).update(eq(userId), any(UserUpdateRequest.class));
            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("should return 400 when update request is invalid")
        void shouldReturn400WhenUpdateRequestIsInvalid() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("", "invalidEmail", "short", "");

            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .put("/api/users/{id}", 1L)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(JsonUtils.asJsonString(request))
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
            UserDto userDto = UserTestUtils.toDtoFrom(UserTestUtils.dummyRegistrationRequest(), userId);
            when(userService.findAll()).thenReturn(List.of(userDto));

            var mvcResult = mockMvc.perform(
                            MockMvcRequestBuilders
                                    .get("/api/users")
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andReturn();
            String responseBody = mvcResult.getResponse().getContentAsString();
            List<UserDto> actual = JsonUtils.fromJsonArrayString(responseBody, UserDto.class);

            assertEquals(1, actual.size());
            assertEquals(userDto, actual.getFirst());
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
            UserDto userDto = UserTestUtils.toDtoFrom(UserTestUtils.dummyRegistrationRequest(), userId);
            when(userService.findById(eq(userId))).thenReturn(Optional.of(userDto));

            var mvcResult = mockMvc.perform(
                            MockMvcRequestBuilders
                                    .get("/api/users/{id}", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isOk())
                    .andReturn();
            String responseBody = mvcResult.getResponse().getContentAsString();
            UserDto actual = JsonUtils.fromJsonString(responseBody, UserDto.class);

            assertEquals(userDto, actual);
            verify(userService).findById(userId);
            verifyNoMoreInteractions(userService);
        }

        @Test
        @DisplayName("should return 404 when user to find by id does not exist")
        void shouldReturn404WhenUserToFindByIdNotExists() throws Exception {
            var userId = 1L;
            when(userService.findById(eq(userId))).thenReturn(Optional.empty());

            mockMvc.perform(
                            MockMvcRequestBuilders
                                    .get("/api/users/{id}", userId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isNotFound());
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
                                    .delete("/api/users/{id}", userId)
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
                                    .delete("/api/users/{id}", unknownUserId)
                                    .contentType(MediaType.APPLICATION_JSON)
                    )
                    .andExpect(status().isNotFound());

            verify(userService).deleteUser(eq(unknownUserId));
            verifyNoMoreInteractions(userService);
        }

    }
}
