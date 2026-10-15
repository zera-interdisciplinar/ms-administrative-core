package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.PasswordResetTokenJpa;

interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpa, UUID> {

    Optional<PasswordResetTokenJpa> findByTokenHash(String tokenHash);
}
