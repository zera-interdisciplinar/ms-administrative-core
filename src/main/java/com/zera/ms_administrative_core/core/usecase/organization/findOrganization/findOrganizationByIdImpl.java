package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;

import java.util.UUID;

public class findOrganizationByIdImpl implements findOrganizationById {

    OrganizationRepository repository;

    public findOrganizationByIdImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrganizationOutput execute(UUID organizationId) {
        Organization org = repository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));
        return OrganizationOutput.from(org);
    }
}
