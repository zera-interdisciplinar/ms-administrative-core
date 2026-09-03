package com.zera.ms_administrative_core.core.usecase.organization.suspendOrganization;

import java.util.UUID;

public interface SuspendOrganization {
    void execute(UUID organizationId);
}
