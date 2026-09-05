package com.zera.ms_administrative_core.core.domain.exception;

/**
 * Lancada quando um refresh token e desconhecido, ja foi revogado ou expirou.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException() {
        super("Refresh token invalido ou expirado");
    }
}
