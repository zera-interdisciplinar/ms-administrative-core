package com.zera.ms_administrative_core.core.usecase.unit.findUnit;

import java.util.UUID;

public interface FindUnitById {
    UnitOutput execute(UUID unitId);
}
