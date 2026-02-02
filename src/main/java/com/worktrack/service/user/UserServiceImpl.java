package com.worktrack.service.user;

import com.worktrack.dto.request.user.RegisterUserRequest;
import com.worktrack.dto.request.user.SearchUserRequest;
import com.worktrack.dto.request.user.UpdateUserRequest;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.exception.user.DuplicateUserException;
import com.worktrack.infra.cache.CacheNames;
import com.worktrack.infra.retry.TransientDbRetry;
import com.worktrack.mapper.UserResponseMapper;
import com.worktrack.repo.specification.Spec;
import com.worktrack.repo.user.UserRepository;
import com.worktrack.repo.user.specification.UserSpecifications;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;



@Service
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserResponseMapper userResponseMapper;
    private final CacheManager cacheManager;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserResponseMapper userMapper, CacheManager cacheManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userResponseMapper = userMapper;
        this.cacheManager = cacheManager;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        validateRegistrationRequest(request);
        String encodedPassword = getEncodedPassword(request.password());
        var user = new User(request.username(), request.email(), encodedPassword, request.fullName(), Role.EMPLOYEE);
        userRepository.save(user);
        return userResponseMapper.toDto(user);
    }

    private void validateRegistrationRequest(RegisterUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateUserException("Email already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateUserException("Username already registered");
        }
    }

    private String getEncodedPassword(String password) {
        return passwordEncoder.encode(password);
    }


    private Optional<UserResponse> findById(Long id) {
        return userRepository.findActiveById(id).map(userResponseMapper::toDto);
    }

    @Cacheable(cacheNames = CacheNames.USERS_BY_USERNAME,
            key = "#username",
            unless = "#result == null")
    @TransientDbRetry
    public Optional<User> findByUsername(String username) {
        return userRepository.findActiveByUsername(username);
    }


    @Transactional(readOnly = true)
    @PreAuthorize("@userPolicy.canListUsers()")
    @TransientDbRetry
    public List<UserResponse> findAll() {
        return userRepository.findAllActives().stream().map(userResponseMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@userPolicy.canListUsersByRole()")
    @TransientDbRetry
    public List<UserResponse> findAllByRole(Role role) {
        return userRepository.findAllActiveByRole(role).stream().map(userResponseMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    @TransientDbRetry
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    @TransientDbRetry
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    @PreAuthorize("@userPolicy.canDeleteUser(#id)")
    public void deleteUser(Long id) {
        User user = findEntityByIdForced(id);
        user.setStatus(Status.DELETED);
        String username = user.getUsername();

        clearUserCache(username);
        userRepository.save(user);
    }

    private void clearUserCache(String username) {
        var byUsername = cacheManager.getCache(CacheNames.USERS_BY_USERNAME);
        if (byUsername != null) byUsername.evict(username);
    }

    @Transactional
    @PreAuthorize("@userPolicy.canUpdateUser(#id)")
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findEntityByIdForced(id);
        clearUserCache(user.getUsername());
        if (request.username() != null) {
            user.setUsername(request.username());
        }
        if (request.email() != null) {
            user.setEmail(request.email());
        }
        if (request.password() != null) {
            user.setPassword(getEncodedPassword(request.password()));
        }
        if (request.fullName() != null) {
            user.setFullName(request.fullName());
        }

        userRepository.save(user);
        return userResponseMapper.toDto(user);
    }

    @Transactional
    @PreAuthorize("@userPolicy.canAssignRole()")
    public UserResponse assignRole(Long id, Role role) {
        User user = findEntityByIdForced(id);
        clearUserCache(user.getUsername());
        user.setRole(role);
        userRepository.save(user);
        return userResponseMapper.toDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("@userPolicy.canReadUser(#id)")
    @TransientDbRetry
    public UserResponse findByIdForced(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    @TransientDbRetry
    public User findEntityByIdForced(Long id) {
        return userRepository.findActiveById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    @TransientDbRetry
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findActiveByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public UserResponse toDto(@Nullable User user) {
        return (user != null) ? userResponseMapper.toDto(user) : null;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("@userPolicy.canSearchUsers()")
    @TransientDbRetry
    public List<UserResponse> search(SearchUserRequest request) {
        Specification<User> spec = Spec.and(
                Spec.whenNotBlank(request.fullName(), UserSpecifications::fullNameContains),
                Spec.whenNotNull(request.role(), UserSpecifications::hasRole),
                Spec.whenNotBlank(request.email(), UserSpecifications::hasEmail),
                UserSpecifications.hasStatus(Status.ACTIVE)
        );

        return userRepository.findAll(spec)
                .stream()
                .map(userResponseMapper::toDto)
                .toList();
    }
}
