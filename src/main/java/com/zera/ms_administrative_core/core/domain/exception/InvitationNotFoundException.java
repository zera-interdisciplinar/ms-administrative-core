package com.zera.ms_administrative_core.core.domain.exception;

public class InvitationNotFoundException extends RuntimeException {
    public InvitationNotFoundException(String code) {
        super("Invitation code not found or already used: " + code);
    }
}
