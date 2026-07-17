package com.zera.ms_administrative_core.core.domain.exception;

import com.zera.ms_administrative_core.core.domain.valueobject.Email;

public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException(Email email) {
        super("Email already in use: " + email);
    }
}