package com.zera.ms_administrative_core.core.usecase.telephone.findTelephone;

import java.util.List;
import java.util.UUID;

public interface FindAllTelephonesByOrganizationId {
    List<TelephoneOutput> execute(UUID organizationId);
}
