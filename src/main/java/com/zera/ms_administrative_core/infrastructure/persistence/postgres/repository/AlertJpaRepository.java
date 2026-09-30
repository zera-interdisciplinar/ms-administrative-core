package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.AlertJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AlertJpaRepository extends JpaRepository<AlertJpa, UUID> {
    Optional<AlertJpa> findByRuleIdAndEventIdAndStatus(UUID ruleId, UUID eventId, AlertStatus status);

    @Query("""
    SELECT a FROM AlertJpa a
    WHERE a.userId = :userId
    AND (:status IS NULL OR a.status = :status)
    ORDER BY a.createdAt DESC
""")
    Page<AlertJpa> findAllByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") AlertStatus status,
            Pageable pageable);
}