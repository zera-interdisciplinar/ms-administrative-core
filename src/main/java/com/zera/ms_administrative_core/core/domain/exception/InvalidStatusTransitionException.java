package com.zera.ms_administrative_core.core.domain.exception;

import com.zera.ms_administrative_core.core.domain.valueobject.Status;

public class InvalidStatusTransitionException extends RuntimeException {
    public InvalidStatusTransitionException(Status from, Status to) {
        super("Invalid transition: " + from + " → " + to);
    }
}