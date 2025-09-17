package com.worktrack.util;

import com.worktrack.dto.request.auth.UserRegistrationRequest;
import com.worktrack.dto.request.auth.UserUpdateRequest;
import com.worktrack.dto.response.user.UserDto;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicInteger;

public class UserTestUtils {

    private static final AtomicInteger counter = new AtomicInteger(1);


    public static User dummyUser() {
        int i = counter.getAndIncrement();
        String username = "testUser" + i;
        String email = "test.user" + i + "@test.com";
        String password = "testPass" + i; // en az 6 karakter
        String fullName = "Test User " + i;

        return new User(username, email, password, fullName, Role.EMPLOYEE);
    }

    public static User dummyUserWithId(Long id) {
        User user = dummyUser();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static UserDto dummyUserDto() {
        return new UserDto(1L, "Test Dto", "test.dto@test.com", "Test Dto", "EMPLOYEE", null, null);
    }

    public static UserRegistrationRequest dummyRegistrationRequest() {
        int id = counter.getAndIncrement(); // her çağrıda artan bir sayı üretir
        String username = "user" + id;
        String email = "user" + id + "@example.com";
        String password = "password" + id;
        String fullName = "Test User " + id;

        return new UserRegistrationRequest(username, email, password, fullName, Role.EMPLOYEE);
    }


    public static UserRegistrationRequest dummyInvalidRegistrationRequest() {
        return new UserRegistrationRequest("", "invalidEmail.test.com", "short", "", Role.EMPLOYEE);
    }
    public static UserUpdateRequest dummyUpdateRequest() {
        int id = counter.getAndIncrement();
        return new UserUpdateRequest(
                "updatedUser" + id,
                "updated" + id + "@test.com",
                "updatedPassword" + id,
                "Updated User " + id
        );
    }

    public static User userWithRole(Role role) {
        int id = counter.getAndIncrement();
        return new User(
                "roleUser" + id,
                "role" + id + "@test.com",
                "rolePass" + id,
                "Role User " + id,
                role
        );
    }

    public static UserDto toDtoFrom(UserRegistrationRequest request, Long id) {
        return new UserDto(
                id,
                request.username(),
                request.email(),
                request.fullName(),
                request.role().name(),
                null,
                null
        );
    }

    public static UserDto toDtoFrom(UserUpdateRequest request, Long id) {
        return new UserDto(
                id,
                request.username(),
                request.email(),
                request.fullName(),
                null,
                null,
                null
        );
    }

    public static UserRegistrationRequest dummyRegisterRequestWithEmail(String email) {
        int id = counter.incrementAndGet();
        return new UserRegistrationRequest(
                "user" + id,
                email,
                "Password" + id,
                "Test User " + id,
                Role.EMPLOYEE
        );
    }

    public static UserRegistrationRequest dummyRegisterRequestWithUsername(String username) {
        int id = counter.incrementAndGet();
        return new UserRegistrationRequest(
                username,
                "test" + id + "@example.com",
                "Password" + id,
                "Test User " + id,
                Role.EMPLOYEE
        );
    }

}