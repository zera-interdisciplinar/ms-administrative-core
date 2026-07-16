package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UserJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface UserJpaRepository extends JpaRepository<UserJpa, UUID> {
    Optional<UserJpa> findByEmail(String email);
    boolean existsByEmail(String email);
}
