package com.zera.ms_administrative_core.core.usecase.organization.renameOrganization;

import java.util.UUID;

public interface RenameOrganization {
    void execute(UUID organizationId, String newName);
}
