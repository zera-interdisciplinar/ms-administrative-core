package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zera.ms_administrative_core.core.domain.valueobject.InvitationStatus;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.InvitationJpa;

interface InvitationJpaRepository extends JpaRepository<InvitationJpa, UUID> {
    Optional<InvitationJpa> findByCodeAndStatus(String code, InvitationStatus status);

    // expiresAt > now exclui convites vencidos que ainda nao foram marcados USED
    // (nao existe status EXPIRED - vencimento e so uma checagem de tempo)
    List<InvitationJpa> findAllByManagerIdAndStatusAndExpiresAtAfterOrderByExpiresAtAsc(
            UUID managerId, InvitationStatus status, LocalDateTime now);
}
