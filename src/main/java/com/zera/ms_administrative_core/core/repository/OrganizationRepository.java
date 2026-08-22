package com.zera.ms_administrative_core.core.repository;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository {
    Organization save(Organization organization);
    Organization findById(UUID id);
    Organization findByCnpj(Cnpj cnpj);
    boolean existsByCnpj(Cnpj cnpj);
    boolean existsByEmail(Email email);
    List<Organization> findAll();
    void delete(UUID id);
}
