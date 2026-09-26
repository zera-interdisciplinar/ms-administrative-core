package com.zera.ms_administrative_core.core.domain.exception;

/** Cliente de servico desconhecido ou segredo incorreto. */
public class InvalidServiceCredentialsException extends RuntimeException {
    public InvalidServiceCredentialsException() {
        super("Invalid service credentials");
    }
}
