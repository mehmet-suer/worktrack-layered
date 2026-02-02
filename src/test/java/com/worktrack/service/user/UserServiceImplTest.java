package com.worktrack.service.user;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.worktrack.dto.request.user.UpdateUserRequest;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.exception.user.DuplicateUserException;
import com.worktrack.mapper.UserResponseMapper;
import com.worktrack.repo.user.UserRepository;
import com.worktrack.util.UserTestUtils;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private UserResponseMapper userResponseMapper = new UserResponseMapper();

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    ArgumentCaptor<User> userCaptor;


    @Nested
    @DisplayName("register() method")
    class RegisterTests {

        @Test
        @DisplayName("Valid user should be registered successfully")
        void shouldRegisterUserSuccessfully() {
            // Arrange
            var request = UserTestUtils.dummyRegistrationRequest();
            var encodedPassword = "encodedPassword";
            when(userRepository.existsByUsername(request.username())).thenReturn(false);
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn(encodedPassword);

            when(userRepository.save(any())).thenAnswer(invocation -> {
                User user = invocation.getArgument(0);
                ReflectionTestUtils.setField(user, "id", 1L);
                return user;
            });

            // Act
            UserResponse response = userService.register(request);

            // Assert
            assertEquals(request.username(), response.username());
            assertEquals(request.fullName(), response.fullName());
            assertEquals(request.email(), response.email());
            assertEquals(Role.EMPLOYEE.name(), response.role());
            assertEquals(1L, response.id());

            // verify
            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertEquals(request.username(), capturedUser.getUsername());
            assertEquals(encodedPassword, capturedUser.getPassword());
            verify(userResponseMapper).toDto(any());
            verifyNoMoreInteractions(userRepository, userResponseMapper);
        }

        @Test
        @DisplayName("User with same email should not be registered")
        void shouldRejectRegistrationWhenEmailExists() {
            // Arrange
            var request = UserTestUtils.dummyRegistrationRequest();
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            // Ac

            DuplicateUserException exception = assertThrowsExactly(DuplicateUserException.class,
                    () -> userService.register(request)
            );

            // Assert
            assertEquals("Email already registered", exception.getMessage());
            verify(userRepository).existsByEmail(request.email());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("User with same username should not be registered")
        void shouldRejectRegistrationWhenUsernameExists() {
            // Arrange
            var request = UserTestUtils.dummyRegistrationRequest();
            when(userRepository.existsByUsername(request.username())).thenReturn(true);
            when(userRepository.existsByEmail(request.email())).thenReturn(false);

            // Act
            DuplicateUserException exception = assertThrowsExactly(DuplicateUserException.class,
                    () -> userService.register(request)
            );

            // Assert
            assertEquals("Username already registered", exception.getMessage());
            verify(userRepository).existsByUsername(request.username());
            verify(userRepository, never()).save(any());
        }
    }


    @Test
    void shouldUpdateUserSuccessfully() {
        // Arrange
        Long id = 1L;
        var user = UserTestUtils.dummyUserWithId(id);
        UpdateUserRequest request = UserTestUtils.dummyUpdateRequest();
        when(userRepository.findActiveById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPass");

        // Act
        UserResponse updatedUser = userService.update(id, request);

        // Assert
        assertEquals(request.username(), updatedUser.username());
        assertEquals(request.email(), updatedUser.email());
        assertEquals(request.fullName(), updatedUser.fullName());

        verify(userRepository).findActiveById(id);
        verify(userResponseMapper).toDto(user);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingUser() {
        // Arrange
        Long id = 1L;

        UpdateUserRequest request = UserTestUtils.dummyUpdateRequest();
        when(userRepository.findActiveById(id)).thenReturn(Optional.empty());

        // Act
        EntityNotFoundException exception = assertThrowsExactly(EntityNotFoundException.class,
                () -> userService.update(id, request)
        );
        // Assert
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findActiveById(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnUserList() {
        // Arrange

        var user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findAllActives()).thenReturn(List.of(user));

        // Act
        var users = userService.findAll();

        // Assert
        assertEquals(1, users.size());
        assertEquals(user.getRole().name(), users.getFirst().role());
        verify(userRepository).findAllActives();
        verify(userResponseMapper).toDto(user);
    }

    @Test
    void shouldReturnUserListByRole() {
        // Arrange
        var role = Role.EMPLOYEE;
        var user = UserTestUtils.userWithRole(role);
        when(userRepository.findAllActiveByRole(role)).thenReturn(List.of(user));

        // Act
        var users = userService.findAllByRole(Role.EMPLOYEE);

        // Assert
        assertEquals(1, users.size());
        assertEquals(role.name(), users.getFirst().role());
        verify(userRepository).findAllActiveByRole(role);
        verify(userResponseMapper).toDto(user);
    }


    @Test
    void shouldReturnUserWhenFoundByUsername() {
        // Arrange

        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findActiveByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByUsername(user.getUsername());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(user.getUsername(), result.get().getUsername());
        verifyNoInteractions(userResponseMapper);
        verify(userRepository).findActiveByUsername(user.getUsername());
    }

    @Test
    void shouldReturnEmptyWhenUserNotFoundByUsername() {
        // Arrange
        when(userRepository.findActiveByUsername(any(String.class)))
                .thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsername("mehmet");
        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findActiveByUsername(any(String.class));
        verify(userResponseMapper, never()).toDto(any());

    }


    @Test
    void shouldDeleteUserSuccessfully() {
        // Arrange
        var user = UserTestUtils.dummyUserWithId(1L);

        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(1L);

        // Assert
        assertEquals(Status.DELETED, user.getStatus());
        verify(userRepository).save(user);
    }

    @Test
    void shouldReturnTrueIfUsernameExists() {
        // Arrange
        String username = "mehmet";
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Act
        boolean exists = userService.existsByUsername(username);

        // Assert
        assertTrue(exists);
        verify(userRepository).existsByUsername(username);
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        // Arrange
        String email = "test.te@mail.com";
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Act
        boolean exists = userService.existsByEmail(email);

        // Assert

        assertTrue(exists);
        verify(userRepository).existsByEmail(email);
    }

    @Test
    void shouldFindUserByIdForced() {
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

        // Act
        UserResponse result = userService.findByIdForced(1L);

        // Assert
        assertNotNull(result);
        assertEquals(user.getUsername(), result.username());
        verify(userRepository).findActiveById(1L);
        verify(userResponseMapper).toDto(user);
    }

    @Test
    void shouldThrowWhenUserNotFoundByIdForced() {
        // Arrange
        when(userRepository.findActiveById(any(Long.class))).thenReturn(Optional.empty());

        // Act
        EntityNotFoundException ex = assertThrowsExactly(
                EntityNotFoundException.class,
                () -> userService.findByIdForced(1L)
        );
        // Assert
        assertEquals("User not found", ex.getMessage());

    }

    @Test
    void shouldReturnEntityWhenFoundByIdForced() {
        // Arrange
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

        // Act
        User result = userService.findEntityByIdForced(1L);

        // Assert
        assertNotNull(result);
        assertSame(user, result);
        verify(userRepository).findActiveById(1L);
        verifyNoInteractions(userResponseMapper);
    }

    @Test
    void shouldThrowIfUserDoesNotExistOnForcedLookup() {
        // Arrange
        when(userRepository.findActiveById(any(Long.class))).thenReturn(Optional.empty());

        // Act
        EntityNotFoundException ex = assertThrowsExactly(
                EntityNotFoundException.class,
                () -> userService.findEntityByIdForced(1L)
        );
        // Assert
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void shouldReturnUserDetailsIfUsernameExists() {
        // Arrange
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findActiveByUsername(user.getUsername()))
                .thenReturn(Optional.of(user));

        // Act
        UserDetails result = userService.loadUserByUsername(user.getUsername());

        // Assert
        assertEquals(user.getUsername(), result.getUsername());
        assertEquals(user.getPassword(), result.getPassword());
        assertTrue(result.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_EMPLOYEE")));
    }

    @Test
    void shouldThrowUsernameNotFoundExceptionWhenUserMissing() {
        // Arrange
        String username = "unknown";
        when(userRepository.findActiveByUsername(username))
                .thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(username)
        );

        assertEquals("User not found", exception.getMessage());
    }


    @Test
    void shouldConvertUserEntityToUserResponse() {
        // Arrange
        User user = UserTestUtils.dummyUserWithId(1L);
        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getCreatedBy(),
                user.getUpdatedBy()
        );
        when(userResponseMapper.toDto(user)).thenReturn(userResponse);
        // Act
        UserResponse result = userService.toDto(user);

        // Assert
        assertNotNull(result);
        assertEquals(userResponse.username(), result.username());
        assertEquals(userResponse.email(), result.email());
        assertEquals(userResponse.fullName(), result.fullName());
        assertEquals(userResponse.role(), result.role());
        assertEquals(userResponse.createdBy(), result.createdBy());
        assertEquals(userResponse.updatedBy(), result.updatedBy());

        verify(userResponseMapper).toDto(user);
    }

    @Test
    void shouldReturnNullForNullUserInMapping() {
        // Arrange
        User user = null;
        // Act
        UserResponse result = userService.toDto(user);

        // Assert
        assertNull(result);
        verify(userResponseMapper, never()).toDto(any());
    }
}
