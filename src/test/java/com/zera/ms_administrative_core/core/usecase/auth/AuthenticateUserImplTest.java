package com.zera.ms_administrative_core.core.usecase.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.exception.InvalidCredentialsException;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.support.FixedPasswordHasher;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

class AuthenticateUserImplTest {

    private InMemoryUserRepository userRepository;
    private AuthenticateUserImpl authenticateUser;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        authenticateUser = new AuthenticateUserImpl(userRepository, new FixedPasswordHasher());
    }

    private Manager manager(String email, Status status) {
        return new Manager(UUID.randomUUID(), "Alice", new Email(email),
                new HashedPassword("hashed:secret"), status, UUID.randomUUID(),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void returnsAuthenticatedUserOnValidCredentials() {
        Manager m = manager("alice@empresa.com", Status.ACTIVE);
        userRepository.save(m);

        AuthenticatedUser result = authenticateUser.execute("alice@empresa.com", "secret");

        assertThat(result.userId()).isEqualTo(m.getUserId());
        assertThat(result.email()).isEqualTo("alice@empresa.com");
        assertThat(result.role()).isEqualTo(Role.MANAGER);
    }

    @Test
    void normalizesEmailBeforeLookup() {
        userRepository.save(manager("alice@empresa.com", Status.ACTIVE));

        assertThat(authenticateUser.execute("  ALICE@empresa.com ", "secret")).isNotNull();
    }

    @Test
    void rejectsUnknownEmail() {
        assertThatThrownBy(() -> authenticateUser.execute("ghost@empresa.com", "secret"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsWrongPassword() {
        userRepository.save(manager("alice@empresa.com", Status.ACTIVE));

        assertThatThrownBy(() -> authenticateUser.execute("alice@empresa.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsMalformedEmailWithoutLeakingParsingError() {
        assertThatThrownBy(() -> authenticateUser.execute("not-an-email", "secret"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsSuspendedAccount() {
        userRepository.save(manager("alice@empresa.com", Status.SUSPENDED));

        assertThatThrownBy(() -> authenticateUser.execute("alice@empresa.com", "secret"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void rejectsNullPassword() {
        userRepository.save(manager("alice@empresa.com", Status.ACTIVE));

        assertThatThrownBy(() -> authenticateUser.execute("alice@empresa.com", null))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
