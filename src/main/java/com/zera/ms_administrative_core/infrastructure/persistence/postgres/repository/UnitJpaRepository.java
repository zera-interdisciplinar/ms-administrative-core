package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UnitJpa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UnitJpaRepository extends JpaRepository<UnitJpa, UUID> {

}
