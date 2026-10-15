package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Token opaco de recuperacao de senha. Apenas o hash do valor bruto e guardado; o valor em si so
 * existe no link enviado por e-mail. Diferente do refresh token, nao e revogavel em massa: ou
 * expira, ou e consumido uma unica vez (usedAt).
 */
public class PasswordResetToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private LocalDateTime usedAt;

    public PasswordResetToken(UUID id, UUID userId, String tokenHash,
            LocalDateTime expiresAt, LocalDateTime createdAt, LocalDateTime usedAt) {
        this.id = Objects.requireNonNull(id);
        this.userId = Objects.requireNonNull(userId);
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.expiresAt = Objects.requireNonNull(expiresAt);
        this.createdAt = Objects.requireNonNull(createdAt);
        this.usedAt = usedAt;
    }

    public static PasswordResetToken issue(UUID userId, String tokenHash, LocalDateTime expiresAt) {
        return new PasswordResetToken(UUID.randomUUID(), userId, tokenHash, expiresAt,
                LocalDateTime.now(), null);
    }

    public boolean isUsableAt(LocalDateTime moment) {
        return usedAt == null && expiresAt.isAfter(moment);
    }

    public void markUsed() {
        this.usedAt = LocalDateTime.now();
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

    public LocalDateTime getUsedAt() {
        return usedAt;
    }
}
