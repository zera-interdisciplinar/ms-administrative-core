package com.zera.ms_administrative_core.core.usecase.auth;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.repository.RefreshTokenRepository;

@Service
public class LogoutImpl implements Logout {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;

    public LogoutImpl(RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
    }

    @Override
    public void execute(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(refreshTokenGenerator.hash(rawRefreshToken))
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
    }
}
