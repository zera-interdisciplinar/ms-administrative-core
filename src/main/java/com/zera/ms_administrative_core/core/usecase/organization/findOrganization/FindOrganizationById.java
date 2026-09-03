package com.zera.ms_administrative_core.core.usecase.organization.findOrganization;

import java.util.UUID;

public interface FindOrganizationById {
    OrganizationOutput execute(UUID organizationId);
}
