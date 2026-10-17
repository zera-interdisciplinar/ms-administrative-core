package com.zera.ms_administrative_core.support;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.zera.ms_administrative_core.core.domain.valueobject.PasswordResetEmail;
import com.zera.ms_administrative_core.core.repository.PasswordResetMailGateway;
import com.zera.ms_administrative_core.core.usecase.auth.passwordReset.PasswordResetLinkFactory;
import com.zera.ms_administrative_core.core.usecase.auth.passwordReset.PasswordResetTokenGenerator;

/** Implementacoes triviais das portas de recuperacao de senha, para teste de caso de uso. */
public final class FakePasswordReset {

    private FakePasswordReset() {
    }

    public static PasswordResetTokenGenerator tokenGenerator() {
        return new PasswordResetTokenGenerator() {
            private final AtomicInteger counter = new AtomicInteger();

            @Override
            public String newRawToken() {
                return "raw-reset-" + counter.incrementAndGet();
            }

            @Override
            public String hash(String rawToken) {
                return "hash(" + rawToken + ")";
            }

            @Override
            public Duration timeToLive() {
                return Duration.ofMinutes(30);
            }
        };
    }

    public static PasswordResetLinkFactory linkFactory() {
        return rawToken -> "https://app.test/reset-password?token=" + rawToken;
    }

    /** Captura cada e-mail "enviado" para asserção nos testes, em vez de uma chamada real. */
    public static class RecordingMailGateway implements PasswordResetMailGateway {
        private final List<PasswordResetEmail> sent = new ArrayList<>();

        @Override
        public void send(PasswordResetEmail email) {
            sent.add(email);
        }

        public List<PasswordResetEmail> sent() {
            return sent;
        }
    }
}
