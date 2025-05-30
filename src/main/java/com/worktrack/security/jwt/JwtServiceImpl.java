package com.worktrack.security.jwt;

import com.worktrack.dto.security.GeneratedToken;
import com.worktrack.dto.security.TokenType;
import com.worktrack.entity.auth.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.jackson.io.JacksonDeserializer;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;



@Service
public class JwtServiceImpl implements JwtService {

    private final JwtProperties jwtProperties;


    public JwtServiceImpl(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public GeneratedToken generateToken(User user) {
        Instant now = Instant.now();
        var token= Jwts.builder()
                .subject(user.getUsername())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(jwtProperties.expiration())))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
        return new GeneratedToken(token, TokenType.BEARER, System.currentTimeMillis() + jwtProperties.expiration());
    }

    @Override
    public boolean isTokenValid(String token, User user) {
        final String username = extractUsername(token);
        return username.equals(user.getUsername()) && !isTokenExpired(token);
    }

    @Override
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) extractAllClaims(token).get("role");
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
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

    public Claims extractAllClaims(String token) {
        return jwtParser().parseSignedClaims(token).getPayload();
    }

}
