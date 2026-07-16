package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.entity.User;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.HashedPassword;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.EmployeeJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.ManagerJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UserJpa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserMapperTest {

    private final UserMapper mapper = new UserMapper();

    @Test
    @DisplayName("Deve mapear UserJpa (Manager) para Domain User")
    void shouldMapManagerJpaToDomain() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        ManagerJpa managerJpa = new ManagerJpa(id, "Manager Name", "manager@test.com", "hashed_password",
                Status.ACTIVE, unitId, now, now);

        User domain = mapper.toDomain(managerJpa);

        assertEquals(id, domain.getUserId());
        assertEquals("Manager Name", domain.getName());
        assertEquals("manager@test.com", domain.getEmail().value());
        assertEquals("hashed_password", domain.getPassword().hash());
        assertEquals(Status.ACTIVE, domain.getStatus());
        assertEquals(unitId, domain.getUnitId());
        assertEquals(Role.MANAGER, domain.role());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("Deve mapear UserJpa (Employee) para Domain User")
    void shouldMapEmployeeJpaToDomain() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        EmployeeJpa employeeJpa = new EmployeeJpa(id, "Employee Name", "employee@test.com", "hashed_password",
                Status.ACTIVE, unitId, now, now);

        User domain = mapper.toDomain(employeeJpa);

        assertEquals(id, domain.getUserId());
        assertEquals("Employee Name", domain.getName());
        assertEquals("employee@test.com", domain.getEmail().value());
        assertEquals("hashed_password", domain.getPassword().hash());
        assertEquals(Status.ACTIVE, domain.getStatus());
        assertEquals(unitId, domain.getUnitId());
        assertEquals(Role.EMPLOYEE, domain.role());
    }

    @Test
    @DisplayName("Deve mapear Domain User (Manager) para ManagerJpa")
    void shouldMapManagerDomainToJpa() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        User domain = com.zera.ms_administrative_core.core.domain.UserFactory.load(
                Role.MANAGER, id, "Manager Name", new Email("manager@test.com"),
                new HashedPassword("hashed_password"), Status.ACTIVE, unitId, now, now);

        UserJpa jpa = mapper.toJpa(domain);

        assertTrue(jpa instanceof ManagerJpa);
        assertEquals(id, jpa.getId());
        assertEquals("Manager Name", jpa.getName());
        assertEquals("manager@test.com", jpa.getEmail());
        assertEquals("hashed_password", jpa.getPassword());
        assertEquals(Status.ACTIVE, jpa.getStatus());
        assertEquals(unitId, jpa.getUnitId());
        assertEquals(Role.MANAGER, jpa.getRole());
    }

    @Test
    @DisplayName("Deve mapear Domain User (Employee) para EmployeeJpa")
    void shouldMapEmployeeDomainToJpa() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        User domain = com.zera.ms_administrative_core.core.domain.UserFactory.load(
                Role.EMPLOYEE, id, "Employee Name", new Email("employee@test.com"),
                new HashedPassword("hashed_password"), Status.ACTIVE, unitId, now, now);

        UserJpa jpa = mapper.toJpa(domain);

        assertTrue(jpa instanceof EmployeeJpa);
        assertEquals(id, jpa.getId());
        assertEquals(Role.EMPLOYEE, jpa.getRole());
    }
}
