package com.zera.ms_administrative_core.core.usecase.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.domain.entity.RefreshToken;
import com.zera.ms_administrative_core.core.domain.valueobject.AccessOrigin;
import com.zera.ms_administrative_core.core.repository.AccessContextRepository;
import com.zera.ms_administrative_core.core.repository.RefreshTokenRepository;

/**
 * Monta um {@link TokenPair} (access token JWT + refresh token opaco persistido) para um usuario
 * ja autenticado. Compartilhado entre login e refresh.
 *
 * <p>Recebe a {@link AccessOrigin} porque a mesma insercao em refresh_token e um LOGIN para o
 * trigger de DAU quando vem de senha, e um REFRESH quando vem de renovacao. Sem essa informacao
 * chegando ao banco, a dimensao de origem da camada analitica nao distingue nada.
 */
@Component
public class SessionTokenFactory {

    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessContextRepository accessContext;

    public SessionTokenFactory(AccessTokenIssuer accessTokenIssuer,
            RefreshTokenGenerator refreshTokenGenerator,
            RefreshTokenRepository refreshTokenRepository,
            AccessContextRepository accessContext) {
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessContext = accessContext;
    }

    public TokenPair issueFor(AuthenticatedUser user, AccessOrigin origin) {
        String accessToken = accessTokenIssuer.issue(user);

        String rawRefreshToken = refreshTokenGenerator.newRawToken();
        LocalDateTime expiresAt = LocalDateTime.now().plus(refreshTokenGenerator.timeToLive());

        // A ordem importa: a origem precisa estar publicada ANTES do INSERT, porque e o INSERT que
        // dispara o trigger de DAU, e ele le a origem naquele exato momento.
        accessContext.bindOrigin(origin);
        refreshTokenRepository.save(RefreshToken.issue(
                user.userId(),
                refreshTokenGenerator.hash(rawRefreshToken),
                expiresAt));

        return TokenPair.bearer(user.userId(), accessToken, rawRefreshToken,
                accessTokenIssuer.timeToLive().toSeconds());
    }
}
