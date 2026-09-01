package com.zera.ms_administrative_core.core.domain.exception;

import java.util.UUID;

public class UnitNotFoundException extends RuntimeException {
    public UnitNotFoundException(UUID id) {
        super("Unit not found: " + id);
    }
}
