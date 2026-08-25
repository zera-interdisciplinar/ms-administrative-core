package com.zera.ms_administrative_core.core.usecase.unit.findUnit;

import java.util.List;
import java.util.UUID;

public interface FindAllUnitsByOrganization {
    List<UnitOutput> execute(UUID organizationId);
}
