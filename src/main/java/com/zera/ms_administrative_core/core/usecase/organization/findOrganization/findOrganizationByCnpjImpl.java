package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;

public class findOrganizationByCnpjImpl implements findOrganizationByCnpj {

    OrganizationRepository repository;

    public findOrganizationByCnpjImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrganizationOutput execute(String cnpj) {
        Organization org = repository.findByCnpj(new Cnpj(cnpj))
                .orElseThrow(() -> new OrganizationNotFoundException(new Cnpj(cnpj)));

        return OrganizationOutput.from(org);
    }
}
