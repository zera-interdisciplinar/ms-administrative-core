package com.zera.ms_administrative_core.core.repository;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationRepository {
    Organization save(Organization organization);
    Optional<Organization> findById(UUID id);
    Optional<Organization> findByCnpj(Cnpj cnpj);
    Optional<Organization> findByEmail(Email email);
    boolean existsByCnpj(Cnpj cnpj);
    boolean existsByEmail(Email email);
    List<Organization> findAll(Plan plan, Status status, int page, int size);
    void delete(UUID id);
}
