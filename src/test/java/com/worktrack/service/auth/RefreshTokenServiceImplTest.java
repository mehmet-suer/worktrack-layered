package com.worktrack.service.auth;

import com.worktrack.dto.security.GeneratedToken;
import com.worktrack.dto.security.TokenType;
import com.worktrack.entity.auth.RefreshToken;
import com.worktrack.entity.auth.User;
import com.worktrack.exception.EntityNotFoundException;
import com.worktrack.exception.auth.InvalidRefreshTokenException;
import com.worktrack.repo.auth.RefreshTokenRepository;
import com.worktrack.security.jwt.JwtProperties;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceImplTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserService userService;

    @Captor
    private ArgumentCaptor<RefreshToken> refreshTokenCaptor;

    private RefreshTokenServiceImpl refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(
                refreshTokenRepository,
                userService,
                new JwtProperties("secret", 600_000L, 2_592_000_000L)
        );
    }

    @Test
    @DisplayName("issueRefreshToken should persist only a hashed token")
    void issueRefreshTokenShouldPersistHash() {
        User user = UserTestUtils.dummyUserWithId(1L);

        GeneratedToken token = refreshTokenService.issueRefreshToken(user);

        assertEquals(TokenType.REFRESH, token.type());
        assertThat(token.token()).isNotBlank();

        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken saved = refreshTokenCaptor.getValue();
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getTokenHash()).isNotBlank().hasSize(64);
        assertThat(saved.getTokenHash()).isNotEqualTo(token.token());
        assertThat(saved.getExpiresAt()).isAfter(Instant.now().minusSeconds(1));
    }

    @Test
    @DisplayName("rotate should revoke old token and issue a new refresh token")
    void rotateShouldRevokeAndIssueNewToken() {
        User user = UserTestUtils.dummyUserWithId(1L);
        String oldRawToken = "old-refresh-token";
        RefreshToken existing = new RefreshToken(user, hashToken(oldRawToken), Instant.now().plusSeconds(60));
        ReflectionTestUtils.setField(existing, "id", 99L);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(userService.requireActiveById(1L)).thenReturn(user);
        when(refreshTokenRepository.revokeIfActive(eq(99L), any(), any())).thenReturn(1);

        RefreshTokenService.RotationResult result = refreshTokenService.rotate(oldRawToken);

        assertThat(result.user()).isSameAs(user);
        assertThat(result.refreshToken().type()).isEqualTo(TokenType.REFRESH);
        assertThat(result.refreshToken().token()).isNotEqualTo(oldRawToken);
        verify(refreshTokenRepository).revokeIfActive(eq(99L), any(), any());
    }

    @Test
    @DisplayName("rotate should reject a token that loses the single-winner race")
    void rotateShouldRejectWhenAtomicRevokeFails() {
        User user = UserTestUtils.dummyUserWithId(1L);
        String oldRawToken = "old-refresh-token";
        RefreshToken existing = new RefreshToken(user, hashToken(oldRawToken), Instant.now().plusSeconds(60));
        ReflectionTestUtils.setField(existing, "id", 77L);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(userService.requireActiveById(1L)).thenReturn(user);
        when(refreshTokenRepository.revokeIfActive(eq(77L), any(), any())).thenReturn(0);

        assertThrowsExactly(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(oldRawToken)
        );

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("rotate should reject token when owning user is no longer active")
    void rotateShouldRejectWhenOwningUserIsInvalid() {
        User user = UserTestUtils.dummyUserWithId(1L);
        String oldRawToken = "old-refresh-token";
        RefreshToken existing = new RefreshToken(user, hashToken(oldRawToken), Instant.now().plusSeconds(60));
        ReflectionTestUtils.setField(existing, "id", 55L);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(userService.requireActiveById(1L)).thenThrow(new EntityNotFoundException("User not found"));

        assertThrowsExactly(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(oldRawToken)
        );

        verify(refreshTokenRepository, never()).revokeIfActive(eq(55L), any(), any());
    }

    @Test
    @DisplayName("rotate should reject expired refresh token")
    void rotateShouldRejectExpiredToken() {
        User user = UserTestUtils.dummyUserWithId(1L);
        String oldRawToken = "old-refresh-token";
        RefreshToken existing = new RefreshToken(user, hashToken(oldRawToken), Instant.now().minusSeconds(60));
        ReflectionTestUtils.setField(existing, "id", 88L);

        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(existing));
        when(userService.requireActiveById(1L)).thenReturn(user);
        when(refreshTokenRepository.revokeIfActive(eq(88L), any(), any())).thenReturn(0);

        assertThrowsExactly(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.rotate(oldRawToken)
        );

        verify(refreshTokenRepository).revokeIfActive(eq(88L), any(), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("revoke should reject unknown refresh token")
    void revokeShouldRejectUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThrowsExactly(
                InvalidRefreshTokenException.class,
                () -> refreshTokenService.revoke("missing-token")
        );
    }

    private String hashToken(String rawToken) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))
            );
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
