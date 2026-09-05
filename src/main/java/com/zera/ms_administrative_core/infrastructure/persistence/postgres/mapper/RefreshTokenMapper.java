package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.domain.entity.RefreshToken;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.RefreshTokenJpa;

@Component
public class RefreshTokenMapper {

    public RefreshTokenJpa toJpa(RefreshToken token) {
        return new RefreshTokenJpa(
                token.getId(),
                token.getUserId(),
                token.getTokenHash(),
                token.getExpiresAt(),
                token.isRevoked(),
                token.getCreatedAt());
    }

    public RefreshToken toDomain(RefreshTokenJpa jpa) {
        return new RefreshToken(
                jpa.getId(),
                jpa.getUserId(),
                jpa.getTokenHash(),
                jpa.getExpiresAt(),
                jpa.getCreatedAt(),
                jpa.isRevoked());
    }
}
