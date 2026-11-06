package com.worktrack.security.auth;

import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.service.user.UserService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserDetailsAdapter implements UserDetailsService {
    private final UserService userService;

    public UserDetailsAdapter(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            return userService.requireActiveByUsername(username);
        } catch (EntityNotFoundException ex) {
            throw new UsernameNotFoundException("User not found", ex);
        }
    }
}
