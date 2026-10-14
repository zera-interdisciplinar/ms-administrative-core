package com.zera.ms_administrative_core.core.usecase.auth.passwordReset;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zera.ms_administrative_core.core.domain.entity.PasswordResetToken;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.exception.InvalidPasswordResetTokenException;
import com.zera.ms_administrative_core.core.domain.service.PasswordHasher;
import com.zera.ms_administrative_core.core.domain.valueobject.RawPassword;
import com.zera.ms_administrative_core.core.repository.PasswordResetTokenRepository;
import com.zera.ms_administrative_core.core.repository.RefreshTokenRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;

@Service
public class ConfirmPasswordResetImpl implements ConfirmPasswordReset {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordResetTokenGenerator tokenGenerator;
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenRepository refreshTokenRepository;

    public ConfirmPasswordResetImpl(PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordResetTokenGenerator tokenGenerator,
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            RefreshTokenRepository refreshTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    @Transactional
    public void execute(String rawToken, String newRawPassword) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidPasswordResetTokenException();
        }

        PasswordResetToken token = passwordResetTokenRepository
                .findByTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(InvalidPasswordResetTokenException::new);

        if (!token.isUsableAt(LocalDateTime.now())) {
            throw new InvalidPasswordResetTokenException();
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(InvalidPasswordResetTokenException::new);

        user.changePassword(passwordHasher.hash(new RawPassword(newRawPassword)));
        userRepository.save(user);

        token.markUsed();
        passwordResetTokenRepository.save(token);

        // Trocar a senha invalida qualquer sessao que ainda estivesse de pe -- se alguem mais
        // tinha acesso ao token antigo (o motivo da troca), ele nao deve continuar logado.
        refreshTokenRepository.revokeAllForUser(user.getUserId());
    }
}
