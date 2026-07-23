package com.zera.ms_administrative_core.core.domain.exception;

import com.zera.ms_administrative_core.core.domain.valueobject.Email;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(UUID id) {
        super("User not found: " + id);
    }

    public UserNotFoundException(Email email) {
        super("User not found: " + email);
    }
}