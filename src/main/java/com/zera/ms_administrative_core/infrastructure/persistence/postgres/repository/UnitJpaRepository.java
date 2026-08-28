package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UnitJpa;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface UnitJpaRepository extends JpaRepository<UnitJpa, UUID> {

    Page<UnitJpa> findAllByOrganizationId(UUID organizationId, Pageable pageable);

    // TODO: garantir unicidade do nome da unidade dentro da organização.
    //  Envolve: expor boolean existsByOrganizationIdAndName(UUID, String) aqui,
    //  adicionar o método correspondente ao port core.repository.UnitRepository,
    //  checar em RegisterUnitImpl e RenameUnitImpl lançando exceção de domínio,
    //  e criar migration com CONSTRAINT unit_organization_id_name_unique UNIQUE (organization_id, name).
}
