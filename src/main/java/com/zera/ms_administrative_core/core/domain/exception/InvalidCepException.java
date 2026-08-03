package com.zera.ms_administrative_core.core.domain.exception;

import com.zera.ms_administrative_core.core.domain.valueobject.Cep;

public class InvalidCepException extends RuntimeException {
    public InvalidCepException(String cep) {
        super("invalid cep: " + cep);
    }
}
