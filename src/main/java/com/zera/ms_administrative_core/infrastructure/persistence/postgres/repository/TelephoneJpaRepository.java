package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.TelephoneJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TelephoneJpaRepository extends JpaRepository<TelephoneJpa, UUID> {

    Optional<TelephoneJpa> findByUserId(UUID userId);

    Optional<TelephoneJpa> findByRecyclingBusinessId(UUID recyclingBusinessId);

    Page<TelephoneJpa> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    boolean existsByUserId(UUID userId);

    boolean existsByRecyclingBusinessId(UUID recyclingBusinessId);
}
