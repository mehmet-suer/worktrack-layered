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
import com.worktrack.mapper.UserResponseMapper;
import com.worktrack.repo.specification.Spec;
import com.worktrack.repo.user.UserRepository;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.worktrack.repo.user.specification.UserSpecifications.*;


@Service
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserResponseMapper userResponseMapper;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserResponseMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userResponseMapper = userMapper;
    }

    @Transactional
    public UserResponse register(RegisterUserRequest request) {
        validateRegistrationRequest(request);
        String encodedPassword = getEncodedPassword(request.password());
        var user = new User(request.username(), request.email(), encodedPassword, request.fullName(), request.role());
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


    public Optional<UserResponse> findById(Long id) {
        return userRepository.findById(id).map(userResponseMapper::toDto);
    }

    public Optional<User> findByUsername(String username){
        return userRepository.findByUsername(username);
    }

    public Optional<UserResponse> findByEmail(String email){
        return userRepository.findByEmail(email).map(userResponseMapper::toDto);
    }

    public List<UserResponse> findAll(){
        return userRepository.findAll().stream().map(userResponseMapper::toDto).toList();
    }

    public List<UserResponse> findAllByRole(Role role) {
        return userRepository.findByRole(role).stream().map(userResponseMapper::toDto).toList();
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public void deleteUser(Long id){
        User user = findEntityByIdForced(id);
        user.setStatus(Status.DELETED);
        userRepository.save(user);
    }


    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findEntityByIdForced(id);

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

        // userRepository.save(user);
        return userResponseMapper.toDto(user);

    }

    @Override
    public UserResponse findByIdForced(Long id) {
        return this.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public User findEntityByIdForced(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    public UserResponse toDto(@Nullable User user) {
        return (user != null) ? userResponseMapper.toDto(user) : null;
    }


    public List<UserResponse> search(SearchUserRequest request) {

        Specification<User> spec = Spec.and(
                Specification.<User>unrestricted(),
                fullNameContains(request.fullName()),
                hasRoles(request.role()),
                hasEmail(request.email())
        );

        return userRepository.findAll(spec)
                .stream()
                .map(userResponseMapper::toDto)
                .toList();
    }
}
