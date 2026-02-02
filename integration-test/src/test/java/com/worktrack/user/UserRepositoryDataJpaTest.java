package com.worktrack.user;

import com.worktrack.base.AbstractJpaTest;
import com.worktrack.entity.auth.Role;
import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.Status;
import com.worktrack.repo.user.UserRepository;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UserRepositoryDataJpaTest extends AbstractJpaTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("should return user when username exists")
    void shouldReturnUserWhenUsernameExists() {
        // arrange
        var saved = userRepository.save(UserTestUtils.dummyUser());
        // act
        Optional<User> found = userRepository.findActiveByUsername(saved.getUsername());

        // assert
        assertThat(found).get()
                .extracting(User::getId, User::getEmail)
                .containsExactly(saved.getId(), saved.getEmail());
    }

    @Test
    @DisplayName("should return empty when username does not exist")
    void shouldReturnEmptyWhenUsernameDoesNotExist() {
        // arrange
        var user = UserTestUtils.dummyUser();
        userRepository.save(user);
        // act
        Optional<User> found = userRepository.findActiveByUsername(user.getUsername() + "someText");

        // assert
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("should not return user when status is DELETED")
    void shouldNotReturnUserWhenDeleted() {
        // arrange
        var user = UserTestUtils.dummyUser();
        user.setStatus(Status.DELETED);
        userRepository.save(user);

        // act
        Optional<User> found = userRepository.findActiveByUsername(user.getUsername());

        // assert
        assertThat(found).isEmpty();
    }


    @Test
    @DisplayName("should return users when role matches")
    void shouldReturnUsersWhenRoleMatches() {
        // arrange
        var selectedRole = Role.EMPLOYEE;
        var user1 = UserTestUtils.userWithRole(selectedRole);
        var user2 = UserTestUtils.userWithRole(selectedRole);
        var user3 = UserTestUtils.userWithRole(Role.MANAGER);
        userRepository.saveAll(java.util.List.of(user1, user2, user3));

        // act
        var foundUsers = userRepository.findByRole(selectedRole);

        // assert
        assertThat(foundUsers)
                .hasSize(2)
                .allMatch(u -> u.getRole() == selectedRole);
    }


    @Test
    @DisplayName("should return true when username exists")
    void shouldReturnTrueWhenUsernameExists() {
        // arrange
        var saved = userRepository.save(UserTestUtils.dummyUser());

        // act
        boolean userFound = userRepository.existsByUsername(saved.getUsername());

        // assert
        assertThat(userFound).isTrue();
    }

    @Test
    @DisplayName("should return false when username does not exist")
    void shouldReturnFalseWhenUsernameDoesNotExist() {
        // arrange
        var saved = userRepository.save(UserTestUtils.dummyUser());
        // act
        var found = userRepository.existsByUsername(saved.getUsername() + "someText");

        // assert
        assertThat(found).isFalse();
    }


    @Test
    @DisplayName("should return true when email exists")
    void shouldReturnTrueWhenEmailExists() {
        // arrange
        var saved = userRepository.save(UserTestUtils.dummyUser());

        // act
        boolean userFound = userRepository.existsByEmail(saved.getEmail());

        // assert
        assertThat(userFound).isTrue();
    }

    @Test
    @DisplayName("should return false when email does not exist")
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // arrange
        var saved = userRepository.save(UserTestUtils.dummyUser());

        // act
        var found = userRepository.existsByEmail(saved.getEmail() + "someText");

        // assert
        assertThat(found).isFalse();
    }


}
