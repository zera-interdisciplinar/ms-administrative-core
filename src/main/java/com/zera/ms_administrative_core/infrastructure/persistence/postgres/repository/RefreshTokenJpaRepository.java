package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.RefreshTokenJpa;

interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpa, UUID> {

    Optional<RefreshTokenJpa> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenJpa t SET t.revoked = true "
            + "WHERE t.userId = :userId AND t.revoked = false")
    void revokeAllForUser(@Param("userId") UUID userId);
}
