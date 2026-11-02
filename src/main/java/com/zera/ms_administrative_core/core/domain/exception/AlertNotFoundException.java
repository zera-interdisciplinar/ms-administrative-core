package com.zera.ms_administrative_core.core.domain.exception;

import java.util.UUID;

public class AlertNotFoundException extends RuntimeException {
    public AlertNotFoundException(UUID id) {
        super("Alert not found: " + id);
    }
}
