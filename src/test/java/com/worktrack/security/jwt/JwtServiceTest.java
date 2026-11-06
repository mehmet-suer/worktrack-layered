package com.worktrack.security.jwt;

import com.worktrack.dto.security.TokenType;
import com.worktrack.entity.auth.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new JwtProperties(secret(), 60_000L, 120_000L)
    );

    @Test
    void shouldIssueAndReadAnAccessTokenWithoutJpaIdentity() {
        var token = jwtService.issueAccessToken("alice", Role.MANAGER);

        assertThat(token.type()).isEqualTo(TokenType.BEARER);
        assertThat(token.token()).isNotBlank();
        assertThat(token.expiresAt()).isGreaterThan(System.currentTimeMillis());
        assertThat(jwtService.extractUsername(token.token())).isEqualTo("alice");
    }

    @Test
    void shouldRejectAnInvalidAccessToken() {
        assertThrowsExactly(
                BadCredentialsException.class,
                () -> jwtService.extractUsername("not-a-jwt")
        );
    }

    private static String secret() {
        byte[] key = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(key);
    }
}
