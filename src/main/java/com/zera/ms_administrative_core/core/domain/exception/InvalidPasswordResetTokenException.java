package com.zera.ms_administrative_core.core.domain.exception;

/**
 * Lancada quando um token de recuperacao de senha e desconhecido, ja foi usado ou expirou.
 */
public class InvalidPasswordResetTokenException extends RuntimeException {
    public InvalidPasswordResetTokenException() {
        super("Token de recuperacao de senha invalido ou expirado");
    }
}
