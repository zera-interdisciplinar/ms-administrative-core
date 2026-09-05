package com.zera.ms_administrative_core.core.repository;

import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.RefreshToken;

public interface RefreshTokenRepository {
    void save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void revokeAllForUser(UUID userId);
}
