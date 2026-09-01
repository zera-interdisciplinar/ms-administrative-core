package com.zera.ms_administrative_core.core.domain.exception;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;

import java.util.UUID;

public class TelephoneNotFoundException extends RuntimeException {
    public TelephoneNotFoundException(UUID id) {
        super("Telephone not found: " + id);
    }
}
