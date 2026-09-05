package com.zera.ms_administrative_core.core.usecase.auth;

public interface AuthenticateUser {

    /**
     * Valida email + senha e o estado da conta.
     *
     * @throws com.zera.ms_administrative_core.core.domain.exception.InvalidCredentialsException
     *         se o email for desconhecido, a senha nao conferir ou a conta nao estiver ativa.
     */
    AuthenticatedUser execute(String email, String rawPassword);
}
