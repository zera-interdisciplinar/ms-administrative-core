package com.zera.ms_administrative_core.core.domain.exception;

public class InvitationExpiredException extends RuntimeException {
    public InvitationExpiredException(String code) {
        super("Invitation code expired: " + code);
    }
}
