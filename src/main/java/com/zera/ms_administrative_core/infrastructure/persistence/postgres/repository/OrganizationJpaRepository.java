package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.OrganizationJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

interface OrganizationJpaRepository extends JpaRepository<OrganizationJpa, UUID> {
    Optional<OrganizationJpa> findByCnpj(String cnpj);
    Optional<OrganizationJpa> findByEmail(String email);
    boolean existsByCnpj(String cnpj);
    boolean existsByEmail(String email);

    @Query("""
    SELECT o FROM OrganizationJpa o
    WHERE (:plan IS NULL OR o.plan = :plan)
    AND (:status IS NULL OR o.status = :status)
""")
    Page<OrganizationJpa> findAllByPlanAndStatus(
            @Param("plan") Plan plan,
            @Param("status") Status status,
            Pageable pageable
    );
}
