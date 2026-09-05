package com.zera.ms_administrative_core.core.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.RefreshToken;
import com.zera.ms_administrative_core.core.domain.exception.InvalidCredentialsException;
import com.zera.ms_administrative_core.core.domain.exception.InvalidRefreshTokenException;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.support.FakeAuthTokens;
import com.zera.ms_administrative_core.support.FixedPasswordHasher;
import com.zera.ms_administrative_core.support.InMemoryRefreshTokenRepository;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

class AuthSessionUseCasesTest {

    private InMemoryUserRepository users;
    private InMemoryRefreshTokenRepository refreshTokens;
    private RefreshTokenGenerator refreshTokenGenerator;

    private LoginImpl login;
    private RefreshSessionImpl refreshSession;
    private LogoutImpl logout;

    private Manager alice;

    @BeforeEach
    void setUp() {
        users = new InMemoryUserRepository();
        refreshTokens = new InMemoryRefreshTokenRepository();
        refreshTokenGenerator = FakeAuthTokens.refreshTokenGenerator();

        SessionTokenFactory sessionTokenFactory = new SessionTokenFactory(
                FakeAuthTokens.accessTokenIssuer(), refreshTokenGenerator, refreshTokens);
        AuthenticateUser authenticateUser = new AuthenticateUserImpl(users, new FixedPasswordHasher());

        login = new LoginImpl(authenticateUser, sessionTokenFactory);
        refreshSession = new RefreshSessionImpl(refreshTokens, refreshTokenGenerator, users, sessionTokenFactory);
        logout = new LogoutImpl(refreshTokens, refreshTokenGenerator);

        alice = new Manager(UUID.randomUUID(), "Alice", new Email("alice@empresa.com"),
                new HashedPassword("hashed:secret"), Status.ACTIVE, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now());
        users.save(alice);
    }

    @Test
    void loginIssuesBearerPairAndPersistsHashedRefreshToken() {
        TokenPair pair = login.execute("alice@empresa.com", "secret");

        assertThat(pair.tokenType()).isEqualTo("Bearer");
        assertThat(pair.accessToken()).isEqualTo("access-token-for-" + alice.getUserId());
        assertThat(pair.expiresInSeconds()).isEqualTo(900);
        assertThat(pair.refreshToken()).isEqualTo("raw-1");

        assertThat(refreshTokens.all()).singleElement().satisfies(stored -> {
            assertThat(stored.getTokenHash()).isEqualTo("hash(raw-1)");
            assertThat(stored.getUserId()).isEqualTo(alice.getUserId());
            assertThat(stored.isRevoked()).isFalse();
        });
    }

    @Test
    void loginRejectsBadPassword() {
        assertThatThrownBy(() -> login.execute("alice@empresa.com", "nope"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(refreshTokens.all()).isEmpty();
    }

    @Test
    void refreshRotatesTokenRevokingThePrevious() {
        String firstRefresh = login.execute("alice@empresa.com", "secret").refreshToken();

        TokenPair rotated = refreshSession.execute(firstRefresh);

        assertThat(rotated.refreshToken()).isEqualTo("raw-2");
        RefreshToken old = refreshTokens.findByTokenHash("hash(raw-1)").orElseThrow();
        assertThat(old.isRevoked()).isTrue();
        assertThat(refreshTokens.findByTokenHash("hash(raw-2)").orElseThrow().isRevoked()).isFalse();
    }

    @Test
    void refreshRejectsUnknownToken() {
        assertThatThrownBy(() -> refreshSession.execute("raw-does-not-exist"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRejectsBlankToken() {
        assertThatThrownBy(() -> refreshSession.execute("  "))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRejectsAlreadyRevokedToken() {
        String refresh = login.execute("alice@empresa.com", "secret").refreshToken();
        refreshSession.execute(refresh);

        assertThatThrownBy(() -> refreshSession.execute(refresh))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRejectsExpiredToken() {
        RefreshToken expired = new RefreshToken(UUID.randomUUID(), alice.getUserId(),
                refreshTokenGenerator.hash("stale"), LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now().minusDays(8), false);
        refreshTokens.save(expired);

        assertThatThrownBy(() -> refreshSession.execute("stale"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshRevokesEverythingWhenAccountIsNoLongerActive() {
        String refresh = login.execute("alice@empresa.com", "secret").refreshToken();
        alice.suspend();
        users.save(alice);

        assertThatThrownBy(() -> refreshSession.execute(refresh))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(refreshTokens.all()).allMatch(RefreshToken::isRevoked);
    }

    @Test
    void logoutRevokesTheToken() {
        String refresh = login.execute("alice@empresa.com", "secret").refreshToken();

        logout.execute(refresh);

        assertThat(refreshTokens.findByTokenHash("hash(" + refresh + ")").orElseThrow().isRevoked())
                .isTrue();
    }

    @Test
    void logoutIsSilentForUnknownOrBlankToken() {
        logout.execute("never-issued");
        logout.execute(null);
        assertThat(refreshTokens.all()).isEmpty();
    }
}
