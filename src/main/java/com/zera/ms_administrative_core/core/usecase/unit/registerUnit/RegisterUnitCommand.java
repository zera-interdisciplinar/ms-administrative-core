package com.zera.ms_administrative_core.core.usecase.unit.registerUnit;

import java.util.UUID;

public record RegisterUnitCommand(
        UUID organizationId,
        String name
) {}
