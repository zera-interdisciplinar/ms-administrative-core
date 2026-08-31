package com.zera.ms_administrative_core.core.domain.exception;

public class InvalidTelephoneNumberException extends RuntimeException {
    public InvalidTelephoneNumberException(String telephoneNumber) {
        super("Invalid telephone number: " + telephoneNumber);
    }
}
