package com.zera.ms_administrative_core.core.domain.exception;

public class InvalidCnpjException extends RuntimeException {
    public InvalidCnpjException(String cnpj) {
        super("Invalid CNPJ: " + cnpj);
    }
}
