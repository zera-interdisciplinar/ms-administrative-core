package com.zera.ms_administrative_core.core.usecase.auth.passwordReset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.PasswordResetToken;
import com.zera.ms_administrative_core.core.domain.exception.InvalidPasswordResetTokenException;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.support.FakePasswordReset;
import com.zera.ms_administrative_core.support.FixedPasswordHasher;
import com.zera.ms_administrative_core.support.InMemoryPasswordResetTokenRepository;
import com.zera.ms_administrative_core.support.InMemoryRefreshTokenRepository;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

class ConfirmPasswordResetImplTest {

    private InMemoryUserRepository userRepository;
    private InMemoryPasswordResetTokenRepository tokenRepository;
    private InMemoryRefreshTokenRepository refreshTokenRepository;
    private PasswordResetTokenGenerator tokenGenerator;
    private ConfirmPasswordResetImpl confirmPasswordReset;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        tokenRepository = new InMemoryPasswordResetTokenRepository();
        refreshTokenRepository = new InMemoryRefreshTokenRepository();
        tokenGenerator = FakePasswordReset.tokenGenerator();
        confirmPasswordReset = new ConfirmPasswordResetImpl(tokenRepository, tokenGenerator,
                userRepository, new FixedPasswordHasher(), refreshTokenRepository);
    }

    private Manager manager(Status status) {
        return new Manager(UUID.randomUUID(), "Alice", new Email("alice@empresa.com"),
                new HashedPassword("hashed:old"), status, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    private String issueTokenFor(UUID userId, LocalDateTime expiresAt) {
        String rawToken = tokenGenerator.newRawToken();
        tokenRepository.save(new PasswordResetToken(UUID.randomUUID(), userId,
                tokenGenerator.hash(rawToken), expiresAt, LocalDateTime.now(), null));
        return rawToken;
    }

    @Test
    void changesPasswordAndRevokesSessionsOnValidToken() {
        Manager alice = manager(Status.ACTIVE);
        userRepository.save(alice);
        refreshTokenRepository.save(com.zera.ms_administrative_core.core.domain.entity.RefreshToken
                .issue(alice.getUserId(), "some-hash", LocalDateTime.now().plusDays(1)));

        String rawToken = issueTokenFor(alice.getUserId(), LocalDateTime.now().plusMinutes(30));

        confirmPasswordReset.execute(rawToken, "new-secret");

        assertThat(userRepository.findById(alice.getUserId()).orElseThrow().getPassword().hash())
                .isEqualTo("hashed:new-secret");
        assertThat(tokenRepository.all().get(0).getUsedAt()).isNotNull();
        assertThat(refreshTokenRepository.all()).allMatch(t -> t.isRevoked());
    }

    @Test
    void rejectsUnknownToken() {
        assertThatThrownBy(() -> confirmPasswordReset.execute("ghost-token", "new-secret"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void rejectsBlankToken() {
        assertThatThrownBy(() -> confirmPasswordReset.execute("  ", "new-secret"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void rejectsExpiredToken() {
        Manager alice = manager(Status.ACTIVE);
        userRepository.save(alice);
        String rawToken = issueTokenFor(alice.getUserId(), LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> confirmPasswordReset.execute(rawToken, "new-secret"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }

    @Test
    void rejectsAlreadyUsedToken() {
        Manager alice = manager(Status.ACTIVE);
        userRepository.save(alice);
        String rawToken = issueTokenFor(alice.getUserId(), LocalDateTime.now().plusMinutes(30));

        confirmPasswordReset.execute(rawToken, "new-secret");

        assertThatThrownBy(() -> confirmPasswordReset.execute(rawToken, "another-secret"))
                .isInstanceOf(InvalidPasswordResetTokenException.class);
    }
}
