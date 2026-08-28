package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.core.repository.UnitRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UnitJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.UnitMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UnitRepositoryImpl implements UnitRepository {

    private final UnitJpaRepository jpa;
    private final UnitMapper mapper;

    public UnitRepositoryImpl(UnitJpaRepository jpa, UnitMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public void save(Unit unit) {
        UnitJpa entity = mapper.toJpa(unit);
        jpa.save(entity);
    }

    @Override
    public List<Unit> findAll(UUID organizationId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpa.findAllByOrganizationId(organizationId, pageable).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Unit> findById(UUID id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public void delete(UUID id) {
        jpa.deleteById(id);
    }
}
