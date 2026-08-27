package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.repository.UnitRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UnitRepositoryImpl implements UnitRepository {

    @Override
    public void save(Unit unit) {

    }

    @Override
    public List<Unit> findAll() {
        return null;
    }

    @Override
    public Optional<Unit> findById(UUID id) {
        return Optional.empty();
    }

    @Override
    public void delete(UUID id) {

    }

    @Override
    public List<Unit> findByOrganization(UUID organizationId) {
        return null;
    }
}
