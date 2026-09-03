package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FindAllOrganizationsImpl implements FindAllOrganizations {

    OrganizationRepository repository;

    public FindAllOrganizationsImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<OrganizationOutput> execute(Plan plan, Status status, int page, int size) {
        return repository.findAll(plan, status, page, size).stream()
                .map(OrganizationOutput::from)
                .toList();
    }
}
