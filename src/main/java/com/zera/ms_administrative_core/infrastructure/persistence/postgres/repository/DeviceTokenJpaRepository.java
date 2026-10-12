package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.DeviceTokenJpa;

public interface DeviceTokenJpaRepository extends JpaRepository<DeviceTokenJpa, UUID> {
    Optional<DeviceTokenJpa> findByToken(String token);
    List<DeviceTokenJpa> findAllByUserId(UUID userId);
    void deleteByToken(String token);
}
