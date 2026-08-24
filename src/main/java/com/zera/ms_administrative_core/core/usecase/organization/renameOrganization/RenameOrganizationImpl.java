package com.zera.ms_administrative_core.core.usecase.organization.renameOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RenameOrganizationImpl implements RenameOrganization{
    OrganizationRepository repository;

    public RenameOrganizationImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID organizationId, String newName) {
        Organization org = repository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (org.getName().equals(newName)) {
            return;
        }

        org.rename(newName);
        repository.save(org);
    }
}
