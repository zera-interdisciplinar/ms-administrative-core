package com.zera.ms_administrative_core.core.usecase.changeUserEmail;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.usecase.user.changeUserEmail.ChangeEmailImpl;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

class ChangeEmailImplTest {

    @Test
    void executeShouldChangeEmailWhenItIsAvailable() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = new Manager(
                UUID.fromString("00000000-0000-0000-0000-000000000030"),
                "User",
                new Email("user@example.com"),
                new HashedPassword("hash"),
                Status.ACTIVE,
                UUID.fromString("00000000-0000-0000-0000-000000000031"),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0));
        repository.save(user);

        ChangeEmailImpl useCase = new ChangeEmailImpl(repository);
        useCase.execute(user.getUserId(), "new@example.com");

        assertEquals(new Email("new@example.com"), repository.findById(user.getUserId()).orElseThrow().getEmail());
    }

    @Test
    void executeShouldRejectWhenEmailIsAlreadyInUse() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User existing = new Manager(
                UUID.fromString("00000000-0000-0000-0000-000000000032"),
                "Existing",
                new Email("existing@example.com"),
                new HashedPassword("hash"),
                Status.ACTIVE,
                UUID.fromString("00000000-0000-0000-0000-000000000033"),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0));
        User target = new Manager(
                UUID.fromString("00000000-0000-0000-0000-000000000035"),
                "Target",
                new Email("target@example.com"),
                new HashedPassword("hash"),
                Status.ACTIVE,
                UUID.fromString("00000000-0000-0000-0000-000000000036"),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0));
        repository.save(existing);
        repository.save(target);

        ChangeEmailImpl useCase = new ChangeEmailImpl(repository);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(target.getUserId(), "existing@example.com"));
        assertEquals("Email already in use", exception.getMessage());
    }

    @Test
    void executeShouldRejectWhenUserDoesNotExist() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        ChangeEmailImpl useCase = new ChangeEmailImpl(repository);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(UUID.fromString("00000000-0000-0000-0000-000000000034"), "new@example.com"));
        assertEquals("User not found", exception.getMessage());
    }
}
