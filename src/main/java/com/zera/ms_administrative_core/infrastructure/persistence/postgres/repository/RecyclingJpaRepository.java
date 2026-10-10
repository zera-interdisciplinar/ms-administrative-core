package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.RecyclingBusinessJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface RecyclingJpaRepository extends JpaRepository<RecyclingBusinessJpa, UUID> {
    Optional<RecyclingBusinessJpa> findByCnpj(String cnpj);
    List<RecyclingBusinessJpa> findByPlaceIdIn(Collection<String> placeIds);
    Optional<RecyclingBusinessJpa> findByPlaceId(String placeId);
}
