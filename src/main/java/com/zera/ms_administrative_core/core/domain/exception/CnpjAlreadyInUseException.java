package com.zera.ms_administrative_core.core.domain.exception;

import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;

public class CnpjAlreadyInUseException extends RuntimeException {
    public CnpjAlreadyInUseException(Cnpj cnpj) {
        super("Cnpj already in use: " + cnpj.value());
    }
}
