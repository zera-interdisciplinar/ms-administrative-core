package com.zera.ms_administrative_core.core.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UnitTest {

    private final UUID unitId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();

    @Test
    @DisplayName("Should create unit with automatic timestamps")
    void shouldCreateWithAutomaticTimestamps() {
        Unit unit = new Unit(unitId, "Matriz", organizationId);

        assertEquals(unitId, unit.getUnitId());
        assertEquals("Matriz", unit.getName());
        assertEquals(organizationId, unit.getOrganizationId());
        assertNotNull(unit.getCreatedAt());
        assertNotNull(unit.getUpdatedAt());
    }

    @Test
    @DisplayName("Should preserve dates when using reconstitution constructor")
    void shouldPreserveDatesOnReconstitution() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 10, 0);

        Unit unit = new Unit(unitId, "Matriz", organizationId, createdAt, updatedAt);

        assertEquals(createdAt, unit.getCreatedAt());
        assertEquals(updatedAt, unit.getUpdatedAt());
    }

    @Test
    @DisplayName("Should rename and touch updatedAt")
    void shouldRename() {
        Unit unit = new Unit(unitId, "Matriz", organizationId,
                LocalDateTime.of(2024, 1, 1, 10, 0), LocalDateTime.of(2024, 1, 1, 10, 0));
        LocalDateTime before = unit.getUpdatedAt();

        unit.rename("Filial");

        assertEquals("Filial", unit.getName());
        assertFalse(unit.getUpdatedAt().isBefore(before));
    }

    @Test
    @DisplayName("Should update updatedAt when calling touch")
    void shouldTouch() {
        Unit unit = new Unit(unitId, "Matriz", organizationId,
                LocalDateTime.of(2024, 1, 1, 10, 0), LocalDateTime.of(2024, 1, 1, 10, 0));
        LocalDateTime before = unit.getUpdatedAt();

        unit.touch();

        assertFalse(unit.getUpdatedAt().isBefore(before));
    }
}
