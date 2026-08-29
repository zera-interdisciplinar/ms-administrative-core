package com.zera.ms_administrative_core.core.usecase.organization.changeOrganizationEmail;

import java.util.UUID;

public interface ChangeOrganizationEmail {
    void execute(UUID organizationId, String newEmail);
}
