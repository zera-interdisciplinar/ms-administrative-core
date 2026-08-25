package com.zera.ms_administrative_core.core.usecase.unit.findUnit;

import com.zera.ms_administrative_core.core.domain.entity.Unit;

import java.time.LocalDateTime;
import java.util.UUID;

public record UnitOutput(
        UUID unitId,
        String name,
        UUID organizationId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UnitOutput from(Unit unit) {
        return new UnitOutput(
                unit.getUnitId(),
                unit.getName(),
                unit.getOrganizationId(),
                unit.getCreatedAt(),
                unit.getUpdatedAt()
        );
    }
}
