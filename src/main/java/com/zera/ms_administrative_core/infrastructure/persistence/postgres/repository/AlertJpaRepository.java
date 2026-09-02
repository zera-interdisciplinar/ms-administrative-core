package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.AlertJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlertJpaRepository extends JpaRepository<AlertJpa, UUID> {
    Optional<AlertJpa> findByRuleIdAndEventIdAndStatus(UUID ruleId, UUID eventId, AlertStatus status);
}