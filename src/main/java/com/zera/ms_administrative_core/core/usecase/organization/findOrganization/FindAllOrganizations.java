package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

import java.util.List;

public interface FindAllOrganizations {
    List<OrganizationOutput> execute(Plan plan, Status status, int size, int page);
}
