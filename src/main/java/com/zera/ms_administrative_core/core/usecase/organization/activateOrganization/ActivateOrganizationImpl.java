package com.zera.ms_administrative_core.core.usecase.organization.activateOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ActivateOrganizationImpl implements ActivateOrganization{
    private final OrganizationRepository repository;

    public ActivateOrganizationImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID organizationId) {
        Organization org = repository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        org.activate();
        repository.save(org);
    }
}
