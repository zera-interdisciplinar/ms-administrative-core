package com.zera.ms_administrative_core.core.usecase.organization.deactivateOrganization;

import java.util.UUID;

public interface DeactivateOrganization {
    void execute(UUID organizationId);
}
