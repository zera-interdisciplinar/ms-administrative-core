package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.exception.InvalidStatusTransitionException;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

class UserTest {

    @Test
    void userMutationsShouldUpdateStateAndTouchTimestamp() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 8, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 1, 9, 0);
        Manager user = new Manager(
                UUID.fromString("00000000-0000-0000-0000-000000000010"),
                "Alice",
                new Email("alice@example.com"),
                new HashedPassword("hash-1"),
                Status.ACTIVE,
                UUID.fromString("00000000-0000-0000-0000-000000000011"),
                createdAt,
                updatedAt);

        user.rename("Alicia");
        LocalDateTime afterRename = user.getUpdatedAt();

        user.changeEmail(new Email("alicia@example.com"));
        LocalDateTime afterEmailChange = user.getUpdatedAt();

        user.deactivate();
        LocalDateTime afterDeactivate = user.getUpdatedAt();

        user.activate();
        LocalDateTime afterActivate = user.getUpdatedAt();

        user.suspend();
        LocalDateTime afterSuspend = user.getUpdatedAt();

        user.changePassword(new HashedPassword("hash-2"));

        assertEquals("Alicia", user.getName());
        assertEquals(new Email("alicia@example.com"), user.getEmail());
        assertEquals(Status.SUSPENDED, user.getStatus());
        assertEquals(new HashedPassword("hash-2"), user.getPassword());
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(Role.MANAGER, user.role());

        assertNotEquals(updatedAt, afterRename);
        assertNotEquals(afterRename, afterEmailChange);
        assertNotEquals(afterEmailChange, afterDeactivate);
        assertNotEquals(afterDeactivate, afterActivate);
        assertNotEquals(afterActivate, afterSuspend);
    }

    @Test
    void suspendShouldUseCustomExceptionForInvalidTransitions() {
        Employee user = new Employee(
                UUID.fromString("00000000-0000-0000-0000-000000000014"),
                "Carol",
                new Email("carol@example.com"),
                new HashedPassword("hash-3"),
                Status.INACTIVE,
                UUID.fromString("00000000-0000-0000-0000-000000000015"),
                LocalDateTime.of(2024, 1, 4, 10, 0),
                LocalDateTime.of(2024, 1, 4, 10, 15));

        InvalidStatusTransitionException exception = assertThrows(
                InvalidStatusTransitionException.class,
                user::suspend);

        assertEquals("Invalid transition: INACTIVE → SUSPENDED", exception.getMessage());
        assertEquals(Status.INACTIVE, user.getStatus());
    }

    @Test
    void gettersShouldExposeCurrentValues() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000012");
        UUID unitId = UUID.fromString("00000000-0000-0000-0000-000000000013");
        HashedPassword password = new HashedPassword("hash");
        Email email = new Email("bob@example.com");

        Employee user = new Employee(userId, "Bob", email, password, Status.ACTIVE, unitId,
                LocalDateTime.of(2024, 1, 3, 12, 0),
                LocalDateTime.of(2024, 1, 3, 12, 30));

        assertEquals(userId, user.getUserId());
        assertEquals("Bob", user.getName());
        assertSame(email, user.getEmail());
        assertSame(password, user.getPassword());
        assertEquals(Status.ACTIVE, user.getStatus());
        assertEquals(unitId, user.getUnitId());
        assertEquals(Role.EMPLOYEE, user.role());
    }
}
