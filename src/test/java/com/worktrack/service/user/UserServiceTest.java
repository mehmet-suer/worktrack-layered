package com.worktrack.service.user;

import com.worktrack.dto.request.user.SearchUserRequest;
import com.worktrack.dto.request.user.UpdateUserRequest;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.exception.user.DuplicateUserException;
import com.worktrack.infra.cache.CacheNames;
import com.worktrack.mapper.UserResponseMapper;
import com.worktrack.repo.user.UserRepository;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private UserResponseMapper userResponseMapper = new UserResponseMapper();

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Test
    void shouldRegisterUser() {
        var request = UserTestUtils.dummyRegistrationRequest();
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            ReflectionTestUtils.setField(user, "id", 1L);
            return user;
        });

        UserResponse response = userService.register(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.username()).isEqualTo(request.username());
        assertThat(response.role()).isEqualTo(Role.EMPLOYEE.name());
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void shouldRejectDuplicateEmailBeforeCheckingUsername() {
        var request = UserTestUtils.dummyRegistrationRequest();
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        DuplicateUserException exception = assertThrowsExactly(
                DuplicateUserException.class,
                () -> userService.register(request)
        );

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldRejectDuplicateUsername() {
        var request = UserTestUtils.dummyRegistrationRequest();
        when(userRepository.existsByUsername(request.username())).thenReturn(true);

        DuplicateUserException exception = assertThrowsExactly(
                DuplicateUserException.class,
                () -> userService.register(request)
        );

        assertEquals("Username already registered", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldUpdateActiveUserAndEvictPreviousUsername() {
        Long id = 1L;
        User user = UserTestUtils.dummyUserWithId(id);
        String previousUsername = user.getUsername();
        UpdateUserRequest request = UserTestUtils.dummyUpdateRequest();
        when(userRepository.findActiveById(id)).thenReturn(Optional.of(user));
        when(cacheManager.getCache(CacheNames.USERS_BY_USERNAME)).thenReturn(cache);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");

        UserResponse response = userService.update(id, request);

        assertThat(response.username()).isEqualTo(request.username());
        assertThat(user.getPassword()).isEqualTo("encoded-password");
        verify(cache).evict(previousUsername);
        verify(userRepository).save(user);
    }

    @Test
    void shouldPropagateMissingUserDuringUpdate() {
        UpdateUserRequest request = UserTestUtils.dummyUpdateRequest();
        when(userRepository.findActiveById(1L)).thenReturn(Optional.empty());

        assertThrowsExactly(EntityNotFoundException.class, () -> userService.update(1L, request));

        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldSoftDeleteAndEvictUser() {
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));
        when(cacheManager.getCache(CacheNames.USERS_BY_USERNAME)).thenReturn(cache);

        userService.deleteUser(1L);

        assertThat(user.getStatus()).isEqualTo(Status.DELETED);
        verify(cache).evict(user.getUsername());
        verify(userRepository).save(user);
    }

    @Test
    void shouldAssignRoleAndEvictUser() {
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));
        when(cacheManager.getCache(CacheNames.USERS_BY_USERNAME)).thenReturn(cache);

        UserResponse response = userService.assignRole(1L, Role.ADMIN);

        assertThat(response.role()).isEqualTo(Role.ADMIN.name());
        verify(cache).evict(user.getUsername());
        verify(userRepository).save(user);
    }

    @Test
    void shouldReturnActiveUsers() {
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findAllActives()).thenReturn(List.of(user));

        List<UserResponse> responses = userService.findAll();

        assertThat(responses).singleElement().extracting(UserResponse::id).isEqualTo(1L);
        verify(userRepository).findAllActives();
    }

    @Test
    void shouldReturnActiveUsersByRole() {
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findAllActiveByRole(Role.EMPLOYEE)).thenReturn(List.of(user));

        List<UserResponse> responses = userService.findAllByRole(Role.EMPLOYEE);

        assertThat(responses).singleElement().extracting(UserResponse::role)
                .isEqualTo(Role.EMPLOYEE.name());
    }

    @Test
    void shouldReturnUserResponseById() {
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.findByIdForced(1L);

        assertThat(response.id()).isEqualTo(1L);
        verify(userRepository).findActiveById(1L);
    }

    @Test
    void shouldRequireActiveUserByUsername() {
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userRepository.findActiveByUsername(user.getUsername())).thenReturn(Optional.of(user));

        User result = userService.requireActiveByUsername(user.getUsername());

        assertThat(result).isSameAs(user);
    }

    @Test
    void shouldRejectMissingUserByUsername() {
        when(userRepository.findActiveByUsername("missing")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrowsExactly(
                EntityNotFoundException.class,
                () -> userService.requireActiveByUsername("missing")
        );

        assertThat(exception).hasMessage("User not found");
    }

    @SuppressWarnings("unchecked")
    @Test
    void shouldSearchActiveUsers() {
        User user = UserTestUtils.dummyUserWithId(1L);
        SearchUserRequest request = new SearchUserRequest(user.getEmail(), user.getFullName(), user.getRole());
        when(userRepository.findAll(any(Specification.class))).thenReturn(List.of(user));

        List<UserResponse> responses = userService.search(request);

        assertThat(responses).singleElement().extracting(UserResponse::id).isEqualTo(1L);
        verify(userRepository).findAll(any(Specification.class));
        verifyNoMoreInteractions(cacheManager);
    }
}
