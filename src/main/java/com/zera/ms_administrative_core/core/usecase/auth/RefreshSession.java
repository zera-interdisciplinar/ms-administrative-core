package com.zera.ms_administrative_core.core.usecase.auth;

public interface RefreshSession {

    /**
     * Rotaciona um refresh token valido: revoga o atual e emite um novo par de tokens.
     *
     * @throws com.zera.ms_administrative_core.core.domain.exception.InvalidRefreshTokenException
     *         se o token for desconhecido, revogado ou expirado, ou se a conta nao estiver mais ativa.
     */
    TokenPair execute(String rawRefreshToken);
}
