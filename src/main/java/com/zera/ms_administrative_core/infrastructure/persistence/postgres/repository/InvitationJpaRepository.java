package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zera.ms_administrative_core.core.domain.valueobject.InvitationStatus;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.InvitationJpa;

interface InvitationJpaRepository extends JpaRepository<InvitationJpa, UUID> {
    Optional<InvitationJpa> findByCodeAndStatus(String code, InvitationStatus status);
}
