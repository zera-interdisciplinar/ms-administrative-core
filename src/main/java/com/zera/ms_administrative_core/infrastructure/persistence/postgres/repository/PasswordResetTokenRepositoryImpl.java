package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.zera.ms_administrative_core.core.domain.entity.PasswordResetToken;
import com.zera.ms_administrative_core.core.repository.PasswordResetTokenRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.PasswordResetTokenMapper;

@Repository
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpa;
    private final PasswordResetTokenMapper mapper;

    public PasswordResetTokenRepositoryImpl(PasswordResetTokenJpaRepository jpa,
            PasswordResetTokenMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public void save(PasswordResetToken token) {
        jpa.save(mapper.toJpa(token));
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(mapper::toDomain);
    }
}
