package com.zera.ms_administrative_core.core.repository;

import com.zera.ms_administrative_core.core.domain.entity.Unit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnitRepository {
    void save(Unit unit);
    List<Unit> findAll(UUID organizationId, int page, int size);
    Optional<Unit> findById(UUID id);
    void delete(UUID id);
}
