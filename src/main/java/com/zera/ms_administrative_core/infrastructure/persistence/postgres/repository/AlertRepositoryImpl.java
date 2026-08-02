package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.repository.AlertRepository;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.AlertJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.AlertMapper;
import org.springframework.stereotype.Repository;

@Repository
public class AlertRepositoryImpl implements AlertRepository {

    private final AlertJpaRepository jpa;
    private final AlertMapper mapper;

    public AlertRepositoryImpl(AlertJpaRepository jpa, AlertMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Alert save(Alert alert) {
        AlertJpa saved = jpa.save(mapper.toJpa(alert));
        return mapper.toDomain(saved);
    }
}