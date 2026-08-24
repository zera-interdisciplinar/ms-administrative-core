package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

@Service
public class FindOrganizationByCnpjImpl implements FindOrganizationByCnpj {

    OrganizationRepository repository;

    public FindOrganizationByCnpjImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public OrganizationOutput execute(String cnpj) {
        Organization org = repository.findByCnpj(new Cnpj(cnpj))
                .orElseThrow(() -> new OrganizationNotFoundException(new Cnpj(cnpj)));

        return OrganizationOutput.from(org);
    }
}
