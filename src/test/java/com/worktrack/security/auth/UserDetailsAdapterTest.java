package com.worktrack.security.auth;

import com.worktrack.entity.auth.User;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.service.user.UserService;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsAdapterTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserDetailsAdapter userDetailsAdapter;

    @Test
    void shouldAdaptActiveUserToSpringSecurity() {
        User user = UserTestUtils.dummyUserWithId(1L);
        when(userService.requireActiveByUsername(user.getUsername())).thenReturn(user);

        var details = userDetailsAdapter.loadUserByUsername(user.getUsername());

        assertThat(details).isSameAs(user);
    }

    @Test
    void shouldTranslateMissingUserForSpringSecurity() {
        when(userService.requireActiveByUsername("missing"))
                .thenThrow(new EntityNotFoundException("User not found"));

        UsernameNotFoundException exception = assertThrowsExactly(
                UsernameNotFoundException.class,
                () -> userDetailsAdapter.loadUserByUsername("missing")
        );

        assertThat(exception).hasMessage("User not found");
    }
}
