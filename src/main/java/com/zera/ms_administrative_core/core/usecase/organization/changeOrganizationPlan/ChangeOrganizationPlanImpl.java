package com.zera.ms_administrative_core.core.usecase.organization.changeOrganizationPlan;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.exception.OrganizationNotFoundException;
import com.zera.ms_administrative_core.core.repository.OrganizationRepository;

import java.util.UUID;

public class ChangeOrganizationPlanImpl implements ChangeOrganizationPlan {
    OrganizationRepository repository;

    public ChangeOrganizationPlanImpl(OrganizationRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(UUID organizationId, Plan plan) {
        Organization org = repository.findById(organizationId)
                .orElseThrow(() -> new OrganizationNotFoundException(organizationId));

        if (org.getPlan() == plan) {
            return;
        }

        org.changePlan(plan);
        repository.save(org);
    }
}
