package com.zera.ms_administrative_core.core.usecase.auth;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.domain.entity.RefreshToken;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.InvalidRefreshTokenException;
import com.zera.ms_administrative_core.core.repository.RefreshTokenRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;

@Service
public class RefreshSessionImpl implements RefreshSession {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final UserRepository userRepository;
    private final SessionTokenFactory sessionTokenFactory;

    public RefreshSessionImpl(RefreshTokenRepository refreshTokenRepository,
            RefreshTokenGenerator refreshTokenGenerator,
            UserRepository userRepository,
            SessionTokenFactory sessionTokenFactory) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.userRepository = userRepository;
        this.sessionTokenFactory = sessionTokenFactory;
    }

    @Override
    @Transactional
    public TokenPair execute(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }

        RefreshToken current = refreshTokenRepository
                .findByTokenHash(refreshTokenGenerator.hash(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!current.isUsableAt(LocalDateTime.now())) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(current.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!user.getStatus().isActive()) {
            refreshTokenRepository.revokeAllForUser(user.getUserId());
            throw new InvalidRefreshTokenException();
        }

        current.revoke();
        refreshTokenRepository.save(current);

        return sessionTokenFactory.issueFor(
                new AuthenticatedUser(user.getUserId(), user.getEmail().value(), user.role()));
    }
}
