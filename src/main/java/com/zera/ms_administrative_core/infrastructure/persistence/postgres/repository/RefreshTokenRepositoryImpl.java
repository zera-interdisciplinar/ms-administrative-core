package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.domain.entity.RefreshToken;
import com.zera.ms_administrative_core.core.repository.RefreshTokenRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.RefreshTokenMapper;

@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;
    private final RefreshTokenMapper mapper;

    public RefreshTokenRepositoryImpl(RefreshTokenJpaRepository jpa, RefreshTokenMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public void save(RefreshToken token) {
        jpa.save(mapper.toJpa(token));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllForUser(UUID userId) {
        jpa.revokeAllForUser(userId);
    }
}
