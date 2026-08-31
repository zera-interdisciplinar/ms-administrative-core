package com.zera.ms_administrative_core.core.usecase.organization.changeOrganizationPlan;

import com.zera.ms_administrative_core.core.domain.entity.Plan;

import java.util.UUID;

public interface ChangeOrganizationPlan {
    void execute(UUID organizationId, Plan plan);
}
