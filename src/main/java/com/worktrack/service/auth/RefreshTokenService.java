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
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
class RefreshTokenService {
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final JwtProperties jwtProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                        UserService userService,
                        JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userService = userService;
        this.jwtProperties = jwtProperties;
    }

    public GeneratedToken issueRefreshToken(User user) {
        return createAndStoreRefreshToken(user);
    }

    public RotationResult rotate(String refreshToken) {
        RefreshToken storedToken = findStoredToken(refreshToken);
        User activeUser = validateOwningUser(storedToken);
        revokeStoredToken(storedToken);

        return new RotationResult(activeUser, createAndStoreRefreshToken(activeUser));
    }

    public void revoke(String refreshToken) {
        RefreshToken storedToken = findStoredToken(refreshToken);
        validateOwningUser(storedToken);
        revokeStoredToken(storedToken);
    }

    private GeneratedToken createAndStoreRefreshToken(User user) {
        Instant expiresAt = Instant.now().plusMillis(jwtProperties.refreshExpiration());
        String rawToken = generateRawToken();
        String tokenHash = hashToken(rawToken);

        refreshTokenRepository.save(new RefreshToken(user, tokenHash, expiresAt));
        return new GeneratedToken(rawToken, TokenType.REFRESH, expiresAt.toEpochMilli());
    }

    private RefreshToken findStoredToken(String rawToken) {
        return refreshTokenRepository.findByTokenHash(hashToken(normalizeToken(rawToken)))
                .orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
    }

    private User validateOwningUser(RefreshToken refreshToken) {
        try {
            return userService.requireActiveById(refreshToken.getUser().getId());
        } catch (EntityNotFoundException ex) {
            throw new InvalidRefreshTokenException("Invalid refresh token", ex);
        }
    }

    private void revokeStoredToken(RefreshToken storedToken) {
        Instant now = Instant.now();
        if (refreshTokenRepository.revokeIfActive(storedToken.getId(), now, now) != 1) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
    }

    private String normalizeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException("Invalid refresh token");
        }
        return rawToken.trim();
    }

    private String generateRawToken() {
        byte[] tokenBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    record RotationResult(User user, GeneratedToken refreshToken) {
    }
}
