package com.zera.ms_administrative_core.core.usecase.organization.deactivateOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;

import java.util.UUID;

public class DeactivateOrganizationImpl implements DeactivateOrganization {
    private final OrganizationRepository repository;

    public DeactivateOrganizationImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID organizationId) {
        Organization org = repository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        org.deactivate();
        repository.save(org);
    }
}
