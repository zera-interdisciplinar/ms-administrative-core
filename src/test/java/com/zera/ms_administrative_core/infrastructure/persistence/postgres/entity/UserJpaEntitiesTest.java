package com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserJpaEntitiesTest {

    @Test
    @DisplayName("Deve instanciar ManagerJpa e validar getters")
    void shouldInstantiateManagerJpa() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        ManagerJpa manager = new ManagerJpa(id, "Name", "email@test.com", "pass",
                Status.ACTIVE, unitId, now, now);

        assertEquals(id, manager.getId());
        assertEquals("Name", manager.getName());
        assertEquals("email@test.com", manager.getEmail());
        assertEquals("pass", manager.getPassword());
        assertEquals(Role.MANAGER, manager.getRole());
        assertEquals(Status.ACTIVE, manager.getStatus());
        assertEquals(unitId, manager.getUnitId());
        assertEquals(now, manager.getCreatedAt());
        assertEquals(now, manager.getUpdatedAt());
    }

    @Test
    @DisplayName("Deve instanciar EmployeeJpa e validar getters")
    void shouldInstantiateEmployeeJpa() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        
        EmployeeJpa employee = new EmployeeJpa(id, "Name", "email@test.com", "pass",
                Status.ACTIVE, unitId, now, now);

        assertEquals(id, employee.getId());
        assertEquals(Role.EMPLOYEE, employee.getRole()); 
    }
}
