package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Refresh token opaco e persistido. Apenas o hash do valor bruto e guardado; o valor em si so
 * existe na resposta do login/refresh entregue ao cliente.
 */
public class RefreshToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private boolean revoked;

    public RefreshToken(UUID id, UUID userId, String tokenHash,
            LocalDateTime expiresAt, LocalDateTime createdAt, boolean revoked) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.revoked = revoked;
    }

    public static RefreshToken issue(UUID userId, String tokenHash, LocalDateTime expiresAt) {
        return new RefreshToken(UUID.randomUUID(), userId, tokenHash, expiresAt,
                LocalDateTime.now(), false);
    }

    public boolean isUsableAt(LocalDateTime moment) {
        return !revoked && expiresAt.isAfter(moment);
    }

    public void revoke() {
        this.revoked = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isRevoked() {
        return revoked;
    }
}
