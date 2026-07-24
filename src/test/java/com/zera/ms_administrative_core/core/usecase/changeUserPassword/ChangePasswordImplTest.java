package com.zera.ms_administrative_core.core.usecase.changeUserPassword;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.usecase.user.changeUserPassword.ChangePasswordCommand;
import com.zera.ms_administrative_core.core.usecase.user.changeUserPassword.ChangePasswordImpl;
import com.zera.ms_administrative_core.support.FixedPasswordHasher;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

class ChangePasswordImplTest {

    @Test
    void executeShouldHashAndUpdatePassword() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = new Manager(
                UUID.fromString("00000000-0000-0000-0000-000000000050"),
                "User",
                new Email("password@example.com"),
                new HashedPassword("old"),
                Status.ACTIVE,
                UUID.fromString("00000000-0000-0000-0000-000000000051"),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0));
        repository.save(user);

        ChangePasswordImpl useCase = new ChangePasswordImpl(repository, new FixedPasswordHasher());
        useCase.execute(new ChangePasswordCommand(user.getUserId(), "new-secret", "new-secret"));

        assertEquals(new HashedPassword("hashed:new-secret"), repository.findById(user.getUserId()).orElseThrow().getPassword());
    }

    @Test
    void executeShouldRejectWhenPasswordsDoNotMatch() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        ChangePasswordImpl useCase = new ChangePasswordImpl(repository, new FixedPasswordHasher());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new ChangePasswordCommand(
                        UUID.fromString("00000000-0000-0000-0000-000000000052"),
                        "new-secret",
                        "different")));
        assertEquals("Passwords do not match", exception.getMessage());
    }

    @Test
    void executeShouldRejectWhenUserDoesNotExist() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        ChangePasswordImpl useCase = new ChangePasswordImpl(repository, new FixedPasswordHasher());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> useCase.execute(new ChangePasswordCommand(
                        UUID.fromString("00000000-0000-0000-0000-000000000053"),
                        "new-secret",
                        "new-secret")));
        assertEquals("User not found", exception.getMessage());
    }
}
