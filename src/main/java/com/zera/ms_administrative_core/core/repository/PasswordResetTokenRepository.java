package com.zera.ms_administrative_core.core.repository;

import java.util.Optional;

import com.zera.ms_administrative_core.core.domain.entity.PasswordResetToken;

public interface PasswordResetTokenRepository {
    void save(PasswordResetToken token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
}
