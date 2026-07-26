package com.zera.ms_administrative_core.core.usecase.userLifecycle;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Employee;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.core.usecase.user.activateUser.ActivateUserImpl;
import com.zera.ms_administrative_core.core.usecase.user.deactivateUser.DeactivateUserImpl;
import com.zera.ms_administrative_core.core.usecase.user.suspendUser.SuspendUserImpl;
import com.zera.ms_administrative_core.support.InMemoryUserRepository;

class UserLifecycleUseCasesTest {

    @Test
    void activateShouldSetStatusToActive() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = user(Status.INACTIVE, "lifecycle-activate@example.com", "00000000-0000-0000-0000-000000000060");
        repository.save(user);

        new ActivateUserImpl(repository).execute(user.getUserId());

        assertEquals(Status.ACTIVE, repository.findById(user.getUserId()).orElseThrow().getStatus());
    }

    @Test
    void deactivateShouldSetStatusToInactive() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = user(Status.ACTIVE, "lifecycle-deactivate@example.com", "00000000-0000-0000-0000-000000000061");
        repository.save(user);

        new DeactivateUserImpl(repository).execute(user.getUserId());

        assertEquals(Status.INACTIVE, repository.findById(user.getUserId()).orElseThrow().getStatus());
    }

    @Test
    void suspendShouldSetStatusToSuspended() {
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = user(Status.ACTIVE, "lifecycle-suspend@example.com", "00000000-0000-0000-0000-000000000062");
        repository.save(user);

        new SuspendUserImpl(repository).execute(user.getUserId());

        assertEquals(Status.SUSPENDED, repository.findById(user.getUserId()).orElseThrow().getStatus());
    }

    @Test
    void useCasesShouldRejectMissingUsers() {
        InMemoryUserRepository repository = new InMemoryUserRepository();

        UserNotFoundException activateException = assertThrows(UserNotFoundException.class,
                () -> new ActivateUserImpl(repository).execute(UUID.fromString("00000000-0000-0000-0000-000000000063")));
        UserNotFoundException deactivateException = assertThrows(UserNotFoundException.class,
                () -> new DeactivateUserImpl(repository).execute(UUID.fromString("00000000-0000-0000-0000-000000000064")));
        UserNotFoundException suspendException = assertThrows(UserNotFoundException.class,
                () -> new SuspendUserImpl(repository).execute(UUID.fromString("00000000-0000-0000-0000-000000000065")));

        assertEquals("User not found: 00000000-0000-0000-0000-000000000063", activateException.getMessage());
        assertEquals("User not found: 00000000-0000-0000-0000-000000000064", deactivateException.getMessage());
        assertEquals("User not found: 00000000-0000-0000-0000-000000000065", suspendException.getMessage());
    }

    private static User user(Status status, String email, String userId) {
        return new Employee(
                UUID.fromString(userId),
                "Lifecycle",
                new Email(email),
                new HashedPassword("hash"),
                status,
                UUID.fromString("00000000-0000-0000-0000-000000000066"),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0),
                java.time.LocalDateTime.of(2024, 1, 1, 10, 0));
    }
}
