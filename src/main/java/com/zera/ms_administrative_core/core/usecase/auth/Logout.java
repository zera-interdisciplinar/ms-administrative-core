package com.zera.ms_administrative_core.core.usecase.auth;

public interface Logout {

    /** Revoga o refresh token informado. Idempotente: um token inexistente e ignorado. */
    void execute(String rawRefreshToken);
}
