package com.zera.ms_administrative_core.core.usecase.organization.suspendOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;

import java.util.UUID;

public class SuspendOrganizationImpl implements SuspendOrganization {
    OrganizationRepository repository;

    public SuspendOrganizationImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID organizationId) {
        Organization org = repository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        org.suspend();
        repository.save(org);
    }
}
