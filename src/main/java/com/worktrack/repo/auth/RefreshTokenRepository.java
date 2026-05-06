package com.worktrack.repo.auth;

import com.worktrack.entity.auth.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = "user")
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE RefreshToken rt
               SET rt.revokedAt = :revokedAt
             WHERE rt.id = :id
               AND rt.revokedAt IS NULL
               AND rt.expiresAt > :now
            """)
    int revokeIfActive(@Param("id") Long id,
                       @Param("revokedAt") Instant revokedAt,
                       @Param("now") Instant now);
}
