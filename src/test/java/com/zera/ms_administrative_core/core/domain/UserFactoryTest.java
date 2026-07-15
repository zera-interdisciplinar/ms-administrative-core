package com.zera.ms_administrative_core.core.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Employee;
import com.zera.ms_administrative_core.core.domain.entity.Manager;
import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

class UserFactoryTest {

    @Test
    void createShouldBuildManagerAndEmployee() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID unitId = UUID.fromString("00000000-0000-0000-0000-000000000002");

        User manager = UserFactory.create(
                Role.MANAGER,
                userId,
                "Manager",
                new Email("manager@example.com"),
                new HashedPassword("hash"),
                Status.ACTIVE,
                unitId);

        User employee = UserFactory.create(
                Role.EMPLOYEE,
                userId,
                "Employee",
                new Email("employee@example.com"),
                new HashedPassword("hash"),
                Status.INACTIVE,
                unitId);

        assertInstanceOf(Manager.class, manager);
        assertInstanceOf(Employee.class, employee);
        assertEquals(Role.MANAGER, manager.role());
        assertEquals(Role.EMPLOYEE, employee.role());
    }

    @Test
    void loadShouldRestoreProvidedTimestamps() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID unitId = UUID.fromString("00000000-0000-0000-0000-000000000004");
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 10, 0);

        User user = UserFactory.load(
                Role.EMPLOYEE,
                userId,
                "Loaded",
                new Email("loaded@example.com"),
                new HashedPassword("hash"),
                Status.SUSPENDED,
                unitId,
                createdAt,
                updatedAt);

        assertInstanceOf(Employee.class, user);
        assertEquals(createdAt, user.getCreatedAt());
        assertEquals(updatedAt, user.getUpdatedAt());
    }
}
