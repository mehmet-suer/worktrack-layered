package com.worktrack.service.auth;

import com.worktrack.dto.response.AuthTokensResponse;
import com.worktrack.dto.response.user.UserResponse;
import com.worktrack.dto.security.GeneratedToken;
import com.worktrack.dto.security.TokenType;
import com.worktrack.entity.auth.RefreshToken;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.exception.auth.InvalidCredentialsException;
import com.worktrack.exception.auth.InvalidRefreshTokenException;
import com.worktrack.repo.auth.RefreshTokenRepository;
import com.worktrack.security.auth.CurrentUserResolver;
import com.worktrack.security.jwt.JwtProperties;
import com.worktrack.security.jwt.JwtService;
import com.worktrack.service.user.UserService;
import com.worktrack.util.UserTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CurrentUserResolver currentUserResolver;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        RefreshTokenService refreshTokenService = new RefreshTokenService(
                refreshTokenRepository,
                userService,
                new JwtProperties("secret", 600_000L, 2_592_000_000L)
        );
        var tokenLifecycle = new AuthenticationTokenLifecycle(
                jwtService,
                refreshTokenService
        );
        authService = new AuthService(
                tokenLifecycle,
                userService,
                passwordEncoder,
                currentUserResolver
        );
    }

    @Test
    @DisplayName("authenticate should issue an access token and persist only a hashed refresh token")
    void authenticateShouldIssueTokenPair() {
        User user = UserTestUtils.dummyUser();
        GeneratedToken accessToken = accessToken("access-token", 111L);

        when(userService.requireActiveByUsername(user.getUsername())).thenReturn(user);
        when(passwordEncoder.matches("plain-password", user.getPassword())).thenReturn(true);
        when(jwtService.issueAccessToken(user.getUsername(), user.getRole())).thenReturn(accessToken);

        AuthTokensResponse response = authService.authenticate(user.getUsername(), "plain-password");

        assertEquals("access-token", response.accessToken().token());
        assertEquals("BEARER", response.accessToken().type());
        assertEquals(111L, response.accessToken().expiresAt());
        assertThat(response.refreshToken().token()).isNotBlank();
        assertEquals("REFRESH", response.refreshToken().type());

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken saved = refreshTokenCaptor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(response.refreshToken().token());
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().minusSeconds(1));
    }

    @Test
    @DisplayName("refresh should atomically revoke the old token and issue a new token pair")
    void refreshShouldRotateTokenPair() {
        User user = UserTestUtils.dummyUserWithId(1L);
        String oldRawToken = "old-refresh-token";
        RefreshToken existing = storedToken(user, oldRawToken, 99L, Instant.now().plusSeconds(60));
        GeneratedToken accessToken = accessToken("new-access-token", 333L);

        when(refreshTokenRepository.findByTokenHash(hashToken(oldRawToken))).thenReturn(Optional.of(existing));
        when(userService.requireActiveById(1L)).thenReturn(user);
        when(refreshTokenRepository.revokeIfActive(eq(99L), any(), any())).thenReturn(1);
        when(jwtService.issueAccessToken(user.getUsername(), user.getRole())).thenReturn(accessToken);

        AuthTokensResponse response = authService.refresh(oldRawToken);

        assertEquals("new-access-token", response.accessToken().token());
        assertThat(response.refreshToken().token()).isNotBlank().isNotEqualTo(oldRawToken);
        assertEquals("REFRESH", response.refreshToken().type());
        verify(refreshTokenRepository).revokeIfActive(eq(99L), any(), any());
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getTokenHash())
                .isEqualTo(hashToken(response.refreshToken().token()));
    }

    @Test
    @DisplayName("refresh should reject a token that loses the single-winner race")
    void refreshShouldRejectWhenAtomicRevokeFails() {
        User user = UserTestUtils.dummyUserWithId(1L);
        String rawToken = "old-refresh-token";
        RefreshToken existing = storedToken(user, rawToken, 77L, Instant.now().plusSeconds(60));

        when(refreshTokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(existing));
        when(userService.requireActiveById(1L)).thenReturn(user);
        when(refreshTokenRepository.revokeIfActive(eq(77L), any(), any())).thenReturn(0);

        assertThrowsExactly(InvalidRefreshTokenException.class, () -> authService.refresh(rawToken));

        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("refresh should reject a token whose owner is no longer active")
    void refreshShouldRejectInactiveOwner() {
        User user = UserTestUtils.dummyUserWithId(1L);
        String rawToken = "old-refresh-token";
        RefreshToken existing = storedToken(user, rawToken, 55L, Instant.now().plusSeconds(60));

        when(refreshTokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(existing));
        when(userService.requireActiveById(1L)).thenThrow(new EntityNotFoundException("User not found"));

        assertThrowsExactly(InvalidRefreshTokenException.class, () -> authService.refresh(rawToken));

        verify(refreshTokenRepository, never()).revokeIfActive(any(), any(), any());
        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("refresh should reject an expired refresh token")
    void refreshShouldRejectExpiredToken() {
        User user = UserTestUtils.dummyUserWithId(1L);
        String rawToken = "expired-refresh-token";
        RefreshToken existing = storedToken(user, rawToken, 88L, Instant.now().minusSeconds(60));

        when(refreshTokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(existing));
        when(userService.requireActiveById(1L)).thenReturn(user);
        when(refreshTokenRepository.revokeIfActive(eq(88L), any(), any())).thenReturn(0);

        assertThrowsExactly(InvalidRefreshTokenException.class, () -> authService.refresh(rawToken));

        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("logout should validate the owner and atomically revoke the refresh token")
    void logoutShouldRevokeRefreshToken() {
        User user = UserTestUtils.dummyUserWithId(1L);
        String rawToken = "refresh-token";
        RefreshToken existing = storedToken(user, rawToken, 44L, Instant.now().plusSeconds(60));

        when(refreshTokenRepository.findByTokenHash(hashToken(rawToken))).thenReturn(Optional.of(existing));
        when(userService.requireActiveById(1L)).thenReturn(user);
        when(refreshTokenRepository.revokeIfActive(eq(44L), any(), any())).thenReturn(1);

        authService.logout(rawToken);

        verify(refreshTokenRepository).revokeIfActive(eq(44L), any(), any());
        verifyNoInteractions(jwtService, passwordEncoder, currentUserResolver);
    }

    @Test
    @DisplayName("logout should reject an unknown refresh token")
    void logoutShouldRejectUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(hashToken("missing-token")))
                .thenReturn(Optional.empty());

        assertThrowsExactly(
                InvalidRefreshTokenException.class,
                () -> authService.logout("missing-token")
        );
    }

    @Test
    @DisplayName("authenticate should reject invalid password without issuing tokens")
    void authenticateShouldRejectInvalidPassword() {
        User user = UserTestUtils.dummyUser();

        when(userService.requireActiveByUsername(user.getUsername())).thenReturn(user);
        when(passwordEncoder.matches("wrong-password", user.getPassword())).thenReturn(false);

        assertThrowsExactly(
                InvalidCredentialsException.class,
                () -> authService.authenticate(user.getUsername(), "wrong-password")
        );

        verifyNoInteractions(jwtService, refreshTokenRepository);
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
        verifyNoInteractions(passwordEncoder, jwtService, refreshTokenRepository);
    }

    @Test
    @DisplayName("getCurrentUserInfo should resolve the authenticated user through the user module")
    void getCurrentUserInfoShouldResolveCurrentUser() {
        UserResponse expected = UserTestUtils.dummyUserResponse();
        when(currentUserResolver.getCurrentUserId()).thenReturn(7L);
        when(userService.findByIdForced(7L)).thenReturn(expected);

        assertThat(authService.getCurrentUserInfo()).isEqualTo(expected);
    }

    private GeneratedToken accessToken(String token, long expiresAt) {
        return new GeneratedToken(token, TokenType.BEARER, expiresAt);
    }

    private RefreshToken storedToken(User user, String rawToken, Long id, Instant expiresAt) {
        RefreshToken storedToken = new RefreshToken(user, hashToken(rawToken), expiresAt);
        ReflectionTestUtils.setField(storedToken, "id", id);
        return storedToken;
    }

    private String hashToken(String rawToken) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
