package com.zera.ms_administrative_core.support;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import com.zera.ms_administrative_core.core.usecase.auth.AccessTokenIssuer;
import com.zera.ms_administrative_core.core.usecase.auth.AuthenticatedUser;
import com.zera.ms_administrative_core.core.usecase.auth.RefreshTokenGenerator;

/** Implementacoes triviais de {@link AccessTokenIssuer} e {@link RefreshTokenGenerator} para testes. */
public final class FakeAuthTokens {

    private FakeAuthTokens() {}

    public static AccessTokenIssuer accessTokenIssuer() {
        return new AccessTokenIssuer() {
            @Override
            public String issue(AuthenticatedUser user) {
                return "access-token-for-" + user.userId();
            }

            @Override
            public Duration timeToLive() {
                return Duration.ofMinutes(15);
            }
        };
    }

    public static RefreshTokenGenerator refreshTokenGenerator() {
        return new RefreshTokenGenerator() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public String newRawToken() {
                return "raw-" + counter.incrementAndGet();
            }

            @Override
            public String hash(String rawToken) {
                return "hash(" + rawToken + ")";
            }

            @Override
            public Duration timeToLive() {
                return Duration.ofDays(7);
            }
        };
    }
}
