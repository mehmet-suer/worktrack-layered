package com.worktrack.service.auth;

import com.worktrack.dto.response.AuthTokensResponse;
import com.worktrack.dto.security.GeneratedToken;
import com.worktrack.dto.security.TokenType;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.exception.auth.InvalidCredentialsException;
import com.worktrack.security.auth.CurrentUserResolver;
import com.worktrack.security.jwt.JwtService;
import com.worktrack.service.user.UserService;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("authenticate should return access and refresh tokens")
    void authenticateShouldReturnTokenPair() {
        User user = UserTestUtils.dummyUser();
        GeneratedToken accessToken = new GeneratedToken("access-token", TokenType.BEARER, 111L);
        GeneratedToken refreshToken = new GeneratedToken("refresh-token", TokenType.REFRESH, 222L);

        when(userService.requireActiveByUsername(user.getUsername())).thenReturn(user);
        when(passwordEncoder.matches("plain-password", user.getPassword())).thenReturn(true);
        when(jwtService.generateToken(user)).thenReturn(accessToken);
        when(refreshTokenService.issueRefreshToken(user)).thenReturn(refreshToken);

        AuthTokensResponse response = authService.authenticate(user.getUsername(), "plain-password");

        assertEquals("access-token", response.accessToken().token());
        assertEquals("BEARER", response.accessToken().type());
        assertEquals(111L, response.accessToken().expiresAt());
        assertEquals("refresh-token", response.refreshToken().token());
        assertEquals("REFRESH", response.refreshToken().type());
        assertEquals(222L, response.refreshToken().expiresAt());

        verify(userService).requireActiveByUsername(user.getUsername());
        verify(passwordEncoder).matches("plain-password", user.getPassword());
        verify(jwtService).generateToken(user);
        verify(refreshTokenService).issueRefreshToken(user);
    }

    @Test
    @DisplayName("refresh should issue a new access token from a rotated refresh token")
    void refreshShouldReturnTokenPair() {
        User user = UserTestUtils.dummyUser();
        GeneratedToken accessToken = new GeneratedToken("new-access-token", TokenType.BEARER, 333L);
        GeneratedToken refreshToken = new GeneratedToken("new-refresh-token", TokenType.REFRESH, 444L);

        when(refreshTokenService.rotate("old-refresh-token"))
                .thenReturn(new RefreshTokenService.RotationResult(user, refreshToken));
        when(jwtService.generateToken(user)).thenReturn(accessToken);

        AuthTokensResponse response = authService.refresh("old-refresh-token");

        assertEquals("new-access-token", response.accessToken().token());
        assertEquals("new-refresh-token", response.refreshToken().token());
        assertEquals(444L, response.refreshToken().expiresAt());

        verify(refreshTokenService).rotate("old-refresh-token");
        verify(jwtService).generateToken(user);
    }

    @Test
    @DisplayName("authenticate should reject invalid password")
    void authenticateShouldRejectInvalidPassword() {
        User user = UserTestUtils.dummyUser();

        when(userService.requireActiveByUsername(user.getUsername())).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", user.getPassword())).thenReturn(false);

        assertThrowsExactly(
                InvalidCredentialsException.class,
                () -> authService.authenticate(user.getUsername(), "wrong-password")
        );

        verify(userService).requireActiveByUsername(user.getUsername());
        verify(passwordEncoder).matches("wrong-password", user.getPassword());
        verifyNoInteractions(jwtService, refreshTokenService);
    }

    @Test
    @DisplayName("logout should delegate to refresh token service")
    void logoutShouldDelegate() {
        authService.logout("refresh-token");

        verify(refreshTokenService).revoke("refresh-token");
        verifyNoInteractions(jwtService, userService, passwordEncoder, currentUserResolver);
    }

    @Test
    @DisplayName("authenticate should hide a missing username behind invalid credentials")
    void authenticateShouldRejectMissingUsername() {
        when(userService.requireActiveByUsername("missing"))
                .thenThrow(new EntityNotFoundException("User not found"));

        InvalidCredentialsException exception = assertThrowsExactly(
                InvalidCredentialsException.class,
                () -> authService.authenticate("missing", "password")
        );

        assertEquals("Invalid username or password", exception.getMessage());
        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenService);
    }
}
