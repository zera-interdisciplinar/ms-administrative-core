package com.zera.ms_administrative_core.core.usecase.auth;

import java.time.Duration;

/** Porta para emissao do access token (JWT). Implementada na camada de infraestrutura. */
public interface AccessTokenIssuer {

    /** Assina e retorna um access token para o usuario informado. */
    String issue(AuthenticatedUser user);

    /** Tempo de vida do access token, refletido no campo {@code expiresIn} da resposta. */
    Duration timeToLive();
}
