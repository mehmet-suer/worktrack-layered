package com.worktrack.security.jwt;

import com.worktrack.dto.security.GeneratedToken;
import com.worktrack.dto.security.TokenType;
import com.worktrack.entity.auth.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    public GeneratedToken issueAccessToken(String username, Role role) {
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject(username)
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtProperties.expiration())))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();

        return new GeneratedToken(
                token,
                TokenType.BEARER,
                now.plusMillis(jwtProperties.expiration()).toEpochMilli()
        );
    }

    public String extractUsername(String token) {
        try {
            return validateToken(token).getSubject();
        } catch (io.jsonwebtoken.ExpiredJwtException ex) {
            throw new CredentialsExpiredException("token expired", ex);
        } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ex) {
            throw new BadCredentialsException("token invalid", ex);
        }
    }

    private Claims validateToken(String token) {
        return jwtParser().parseSignedClaims(token).getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.secret());
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private JwtParser jwtParser() {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .json(new JacksonDeserializer<>(Map.of()))
                .build();
    }
}
