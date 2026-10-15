package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.domain.entity.PasswordResetToken;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.PasswordResetTokenJpa;

@Component
public class PasswordResetTokenMapper {

    public PasswordResetTokenJpa toJpa(PasswordResetToken token) {
        return new PasswordResetTokenJpa(
                token.getId(),
                token.getUserId(),
                token.getTokenHash(),
                token.getExpiresAt(),
                token.getUsedAt(),
                token.getCreatedAt());
    }

    public PasswordResetToken toDomain(PasswordResetTokenJpa jpa) {
        return new PasswordResetToken(
                jpa.getId(),
                jpa.getUserId(),
                jpa.getTokenHash(),
                jpa.getExpiresAt(),
                jpa.getCreatedAt(),
                jpa.getUsedAt());
    }
}
