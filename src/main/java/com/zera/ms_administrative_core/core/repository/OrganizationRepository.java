package com.zera.ms_administrative_core.core.repository;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository {
    Organization save(Organization organization);
    Organization findById(UUID id);
    Organization findByCnpj(Cnpj cnpj);
    List<Organization> findAll();
    void delete(UUID id);
}
