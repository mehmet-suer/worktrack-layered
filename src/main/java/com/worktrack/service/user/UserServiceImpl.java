package com.worktrack.service.user;

import com.worktrack.dto.request.auth.UserRegistrationRequest;
import com.worktrack.dto.request.auth.UserUpdateRequest;
import com.worktrack.dto.response.user.UserDto;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.exception.user.DuplicateUserException;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.mapper.UserDtoMapper;
import com.worktrack.repo.specification.GenericSpecificationBuilder;
import com.worktrack.repo.specification.Spec;
import com.worktrack.repo.user.UserRepository;
import com.worktrack.repo.user.specification.UserSpecifications;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
    private final UserDtoMapper userDtoMapper;

    public UserServiceImpl(UserRepository userRepository,
                           PasswordEncoder passwordEncoder,
                           UserDtoMapper userMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.userDtoMapper = userMapper;
    }

    @Transactional
    public UserDto register(UserRegistrationRequest request) {
        validateRegistrationRequest(request);
        String encodedPassword = getEncodedPassword(request.password());
        var user = new User(request.username(), request.email(), encodedPassword, request.fullName(), request.role());
        userRepository.save(user);
        return userDtoMapper.toDto(user);
    }

    private void validateRegistrationRequest(UserRegistrationRequest request) {
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


    public Optional<UserDto> findById(Long id) {
        return userRepository.findById(id).map(userDtoMapper::toDto);
    }

    public Optional<User> findByUsername(String username){
        return userRepository.findByUsername(username);
    }

    public Optional<UserDto> findByEmail(String email){
        return userRepository.findByEmail(email).map(userDtoMapper::toDto);
    }

    public List<UserDto> findAll(){
        return userRepository.findAll().stream().map(userDtoMapper::toDto).toList();
    }

    public List<UserDto> findAllByRole(Role role) {
        return userRepository.findByRole(role).stream().map(userDtoMapper::toDto).toList();
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


    @Transactional
    public UserDto update(Long id, UserUpdateRequest request) {
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
        return userDtoMapper.toDto(user);

    }

    @Override
    public UserDto findByIdForced(Long id) {
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

    public UserDto toDto(@Nullable User user) {
        return (user != null) ? userDtoMapper.toDto(user) : null;
    }


    public List<UserDto> searchUsers(String username, Role role, Status status) {
        Specification<User> spec = Specification
                .where(usernameContains(username))
                .and(hasRole(role))
                .and(hasStatus(status));
        Specification<User> spec2 = new GenericSpecificationBuilder<User>()
                .addIfPresent(username, UserSpecifications::usernameContains)
                .addIfPresent(role, UserSpecifications::hasRole)
                .addIfPresent(status, UserSpecifications::hasStatus)
                .build();

        return userRepository.findAll(spec)
                .stream()
                .map(userDtoMapper::toDto)
                .toList();
    }

    public List<UserDto> search(String keyword, Role role) {
        Specification<User> spec = Spec.and(
                new Specification<User>() {
                    @Override
                    public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                        return null;
                    }
                }
                ,
                Spec.or(
                        Spec.eq("role", Role.EMPLOYEE),
                        Spec.not(Spec.eq("status", Status.DELETED))
                )
        );

        return userRepository.findAll(spec)
                .stream()
                .map(userDtoMapper::toDto)
                .toList();
    }

}
