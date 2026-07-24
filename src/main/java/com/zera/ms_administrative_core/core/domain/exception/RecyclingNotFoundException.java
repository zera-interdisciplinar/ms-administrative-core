package com.zera.ms_administrative_core.core.domain.exception;

import java.util.UUID;

public class RecyclingNotFoundException extends RuntimeException {
    public RecyclingNotFoundException(UUID recyclingId) {
        super("Recycling not found with id: " + recyclingId);
    }
}
