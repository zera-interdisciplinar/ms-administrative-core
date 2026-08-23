package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;

public class findOrganizationByEmailImpl implements  findOrganizationByEmail {
    OrganizationRepository repository;

    public findOrganizationByEmailImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrganizationOutput execute(String email) {

        Organization org = repository.findByEmail(new Email(email))
                .orElseThrow(() -> new OrganizationNotFoundException(new Email(email)));

        return OrganizationOutput.from(org);
    }


}
