package com.zera.ms_administrative_core.core.usecase.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.domain.entity.RefreshToken;
import com.zera.ms_administrative_core.core.repository.RefreshTokenRepository;

/**
 * Monta um {@link TokenPair} (access token JWT + refresh token opaco persistido) para um usuario
 * ja autenticado. Compartilhado entre login e refresh.
 */
@Component
public class SessionTokenFactory {

    private final AccessTokenIssuer accessTokenIssuer;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenRepository refreshTokenRepository;

    public SessionTokenFactory(AccessTokenIssuer accessTokenIssuer,
            RefreshTokenGenerator refreshTokenGenerator,
            RefreshTokenRepository refreshTokenRepository) {
        this.accessTokenIssuer = accessTokenIssuer;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public TokenPair issueFor(AuthenticatedUser user) {
        String accessToken = accessTokenIssuer.issue(user);

        String rawRefreshToken = refreshTokenGenerator.newRawToken();
        LocalDateTime expiresAt = LocalDateTime.now().plus(refreshTokenGenerator.timeToLive());
        refreshTokenRepository.save(RefreshToken.issue(
                user.userId(),
                refreshTokenGenerator.hash(rawRefreshToken),
                expiresAt));

        return TokenPair.bearer(accessToken, rawRefreshToken,
                accessTokenIssuer.timeToLive().toSeconds());
    }
}
