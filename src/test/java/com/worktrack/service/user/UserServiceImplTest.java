package com.worktrack.service.user;


import com.worktrack.dto.request.auth.UserUpdateRequest;
import com.worktrack.dto.response.user.UserDto;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.exception.user.DuplicateUserException;
import com.worktrack.mapper.UserDtoMapper;
import com.worktrack.repo.user.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import com.worktrack.util.UserTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private UserDtoMapper userDtoMapper = new UserDtoMapper();

    @InjectMocks
    private UserServiceImpl userService;

    @Captor
    ArgumentCaptor<User> userCaptor;




    @Nested
    @DisplayName("register() metodu")
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
            UserDto response = userService.register(request);

            // Assert
            assertEquals(request.username(), response.username());
            assertEquals(request.fullName(), response.fullName());
            assertEquals(request.email(), response.email());
            assertEquals(request.role().name(), response.role());
            assertEquals(1L, response.id());

            // verify ile davranış testi
            verify(userRepository).save(userCaptor.capture());
            User capturedUser = userCaptor.getValue();
            assertEquals(request.username(), capturedUser.getUsername());
            assertEquals(encodedPassword, capturedUser.getPassword());
            verify(userDtoMapper).toDto(any());
            verifyNoMoreInteractions(userRepository, userDtoMapper);
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
        UserUpdateRequest request = UserTestUtils.dummyUpdateRequest();
        when(userRepository.findById(id)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(request.password())).thenReturn("encodedPass");

        // Act
        UserDto updatedUser = userService.update(id, request);

        // Assert
        assertEquals(request.username(), updatedUser.username());
        assertEquals(request.email(), updatedUser.email());
        assertEquals(request.fullName(), updatedUser.fullName());

        verify(userRepository).findById(id);
        verify(userDtoMapper).toDto(user);
    }

    @Test
    void shouldThrowExceptionWhenUpdatingNonExistingUser() {
        // Arrange
        Long id = 1L;

        UserUpdateRequest request = UserTestUtils.dummyUpdateRequest();
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        // Act
        EntityNotFoundException exception = assertThrowsExactly(EntityNotFoundException.class,
                () -> userService.update(id, request)
        );
        // Assert
        assertEquals("User not found", exception.getMessage());
        verify(userRepository).findById(id);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnUserList() {
        // Arrange

        var user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findAll()).thenReturn(List.of(user));

        // Act
        var users = userService.findAll();

        // Assert
        assertEquals(1, users.size());
        assertEquals(user.getRole().name(), users.getFirst().role());
        verify(userRepository).findAll();
        verify(userDtoMapper).toDto(user);
    }

    @Test
    void shouldReturnUserListByRole() {
        // Arrange
        var role = Role.EMPLOYEE;
        var user = UserTestUtils.userWithRole(role);
        when(userRepository.findByRole(role)).thenReturn(List.of(user));

        // Act
        var users = userService.findAllByRole(Role.EMPLOYEE);

        // Assert
        assertEquals(1, users.size());
        assertEquals(role.name(), users.getFirst().role());
        verify(userRepository).findByRole(role);
        verify(userDtoMapper).toDto(user);
    }



    @Test
    void shouldReturnUserById() {
        // Arrange
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        Optional<UserDto> result = userService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(user.getUsername(), result.get().username());
        verify(userRepository).findById(1L);
        verify(userDtoMapper).toDto(user);
    }

    @Test
    void shouldReturnEmptyIfUserIdNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        Optional<UserDto> result = userService.findById(1L);
        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findById(1L);
        verify(userDtoMapper, never()).toDto(any());
        verifyNoInteractions(userDtoMapper);
    }

    @Test
    void shouldReturnUserWhenFoundByUsername() {
        // Arrange

        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // Act
        Optional<User> result = userService.findByUsername(user.getUsername());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(user.getUsername(), result.get().getUsername());
        verifyNoInteractions(userDtoMapper);
        verify(userRepository).findByUsername(user.getUsername());
    }

    @Test
    void shouldReturnEmptyWhenUserNotFoundByUsername() {
        // Arrange
        when(userRepository.findByUsername(any(String.class))).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsername("mehmet");
        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findByUsername(any(String.class));
        verify(userDtoMapper, never()).toDto(any());

    }

    @Test
    void shouldReturnUserWhenFoundByEmail() {
        // Arrange
        var user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        // Act
        Optional<UserDto> result = userService.findByEmail(user.getEmail());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(user.getEmail(), result.get().email());
        assertEquals(user.getUsername(), result.get().username());
        assertEquals(user.getFullName(), result.get().fullName());
        assertEquals(user.getRole().name(), result.get().role());

        verify(userRepository).findByEmail(user.getEmail());
        verify(userDtoMapper).toDto(user);
    }

    @Test
    void shouldReturnEmptyResultWhenUserNotFoundByEmail() {
        // Arrange
        String unknownEmail = "unknown@email.com";

        when(userRepository.findByEmail(unknownEmail)).thenReturn(Optional.empty());

        // Act
        Optional<UserDto> result = userService.findByEmail(unknownEmail);

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository).findByEmail(unknownEmail);
        verify(userDtoMapper, never()).toDto(any());
    }




    @Test
    void shouldDeleteUserSuccessfully() {
        // Arrange
        var user = UserTestUtils.dummyUserWithId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

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
        String  email = "test.te@mail.com";
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
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        UserDto result = userService.findByIdForced(1L);

        // Assert
        assertNotNull(result);
        assertEquals(user.getUsername(), result.username());
        verify(userRepository).findById(1L);
        verify(userDtoMapper).toDto(user);
    }

    @Test
    void shouldThrowWhenUserNotFoundByIdForced() {
        // Arrange
        when(userRepository.findById(any(Long.class))).thenReturn(Optional.empty());

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
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        User result = userService.findEntityByIdForced(1L);

        // Assert
        assertNotNull(result);
        assertEquals(user.getUsername(), result.getUsername());
        verify(userRepository).findById(1L);
        verifyNoInteractions(userDtoMapper);
    }

    @Test
    void shouldThrowIfUserDoesNotExistOnForcedLookup() {
        // Arrange
        when(userRepository.findById(any(Long.class))).thenReturn(Optional.empty());

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
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

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
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
                UsernameNotFoundException.class,
                () -> userService.loadUserByUsername(username)
        );

        assertEquals("User not found", exception.getMessage());
    }



    @Test
    void shouldConvertUserEntityToUserDto() {
        // Arrange
        User user = UserTestUtils.dummyUserWithId(1L);
        UserDto userDto = new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getCreatedBy(),
                user.getUpdatedBy()
        );
        when(userDtoMapper.toDto(user)).thenReturn(userDto);
        // Act
        UserDto result = userService.toDto(user);

        // Assert
        assertNotNull(result);
        assertEquals(userDto.username(), result.username());
        assertEquals(userDto.email(), result.email());
        assertEquals(userDto.fullName(), result.fullName());
        assertEquals(userDto.role(), result.role());
        assertEquals(userDto.createdBy(), result.createdBy());
        assertEquals(userDto.updatedBy(), result.updatedBy());

        verify(userDtoMapper).toDto(user);
    }

    @Test
    void shouldReturnNullForNullUserInMapping() {
        // Arrange
        User user = null;
        // Act
        UserDto result = userService.toDto(user);

        // Assert
        assertNull(result);
        verify(userDtoMapper, never()).toDto(any());
    }
}
