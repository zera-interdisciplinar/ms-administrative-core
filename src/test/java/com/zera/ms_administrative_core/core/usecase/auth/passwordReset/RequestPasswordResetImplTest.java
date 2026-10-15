package com.zera.ms_administrative_core.core.usecase.auth.passwordReset;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.support.FakePasswordReset;
import com.zera.ms_administrative_core.support.FakePasswordReset.RecordingMailGateway;
import com.zera.ms_administrative_core.support.InMemoryPasswordResetTokenRepository;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

class RequestPasswordResetImplTest {

    private InMemoryUserRepository userRepository;
    private InMemoryPasswordResetTokenRepository tokenRepository;
    private RecordingMailGateway mailGateway;
    private RequestPasswordResetImpl requestPasswordReset;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        tokenRepository = new InMemoryPasswordResetTokenRepository();
        mailGateway = new RecordingMailGateway();
        requestPasswordReset = new RequestPasswordResetImpl(userRepository, tokenRepository,
                FakePasswordReset.tokenGenerator(), FakePasswordReset.linkFactory(), mailGateway);
    }

    private Manager manager(String email, Status status) {
        return new Manager(UUID.randomUUID(), "Alice", new Email(email),
                new HashedPassword("hashed:secret"), status, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void issuesTokenAndSendsEmailForActiveUser() {
        Manager alice = manager("alice@empresa.com", Status.ACTIVE);
        userRepository.save(alice);

        requestPasswordReset.execute("alice@empresa.com");

        assertThat(tokenRepository.all()).hasSize(1);
        assertThat(tokenRepository.all().get(0).getUserId()).isEqualTo(alice.getUserId());

        assertThat(mailGateway.sent()).hasSize(1);
        assertThat(mailGateway.sent().get(0).to()).isEqualTo("alice@empresa.com");
        assertThat(mailGateway.sent().get(0).resetLink()).contains("token=raw-reset-1");
    }

    @Test
    void doesNothingForUnknownEmailWithoutLeaking() {
        requestPasswordReset.execute("ghost@empresa.com");

        assertThat(tokenRepository.all()).isEmpty();
        assertThat(mailGateway.sent()).isEmpty();
    }

    @Test
    void doesNothingForInactiveUser() {
        userRepository.save(manager("alice@empresa.com", Status.SUSPENDED));

        requestPasswordReset.execute("alice@empresa.com");

        assertThat(tokenRepository.all()).isEmpty();
        assertThat(mailGateway.sent()).isEmpty();
    }

    @Test
    void doesNothingForMalformedEmailWithoutThrowing() {
        requestPasswordReset.execute("not-an-email");

        assertThat(tokenRepository.all()).isEmpty();
        assertThat(mailGateway.sent()).isEmpty();
    }
}
