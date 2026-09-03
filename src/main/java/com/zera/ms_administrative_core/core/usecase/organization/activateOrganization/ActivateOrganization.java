package com.zera.ms_administrative_core.core.usecase.organization.activateOrganization;

import java.util.UUID;

public interface ActivateOrganization {
    void execute(UUID organizationId);
}
