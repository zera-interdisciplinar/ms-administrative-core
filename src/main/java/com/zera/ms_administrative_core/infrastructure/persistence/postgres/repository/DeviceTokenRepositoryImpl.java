package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.zera.ms_administrative_core.core.domain.entity.DeviceToken;
import com.zera.ms_administrative_core.core.repository.DeviceTokenRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.DeviceTokenJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.DeviceTokenMapper;

@Repository
public class DeviceTokenRepositoryImpl implements DeviceTokenRepository {

    private final DeviceTokenJpaRepository jpa;
    private final DeviceTokenMapper mapper;

    public DeviceTokenRepositoryImpl(DeviceTokenJpaRepository jpa, DeviceTokenMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public DeviceToken upsert(DeviceToken deviceToken) {
        Optional<DeviceTokenJpa> existing = jpa.findByToken(deviceToken.getToken());
        if (existing.isPresent()) {
            // mesmo token, possivelmente outro usuario (reinstalacao/troca de login no device) --
            // move o token em vez de duplicar a linha, ver V21.
            DeviceToken found = mapper.toDomain(existing.get());
            found.reassignTo(deviceToken.getUserId(), deviceToken.getPlatform());
            return save(found);
        }
        return save(deviceToken);
    }

    private DeviceToken save(DeviceToken deviceToken) {
        DeviceTokenJpa saved = jpa.save(mapper.toJpa(deviceToken));
        return mapper.toDomain(saved);
    }

    @Override
    public List<DeviceToken> findByUserId(UUID userId) {
        return jpa.findAllByUserId(userId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteByToken(String token) {
        jpa.deleteByToken(token);
    }
}
