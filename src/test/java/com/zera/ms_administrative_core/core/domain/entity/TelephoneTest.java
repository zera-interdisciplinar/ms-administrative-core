package com.zera.ms_administrative_core.core.domain.entity;

import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TelephoneTest {

    private final UUID telephoneId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();
    private final UUID recyclingBusinessId = UUID.randomUUID();
    private final TelephoneNumber number = new TelephoneNumber("11987654321");

    @Test
    @DisplayName("Should create user telephone with automatic timestamps")
    void shouldCreateUserTelephone() {
        Telephone telephone = new Telephone(telephoneId, number, userId, organizationId, unitId);

        assertEquals(telephoneId, telephone.getTelephoneId());
        assertEquals(number, telephone.getNumber());
        assertEquals(userId, telephone.getUserId());
        assertEquals(organizationId, telephone.getOrganizationId());
        assertEquals(unitId, telephone.getUnitId());
        assertNull(telephone.getRecyclingBusinessId());
        assertNotNull(telephone.getCreatedAt());
        assertNotNull(telephone.getUpdatedAt());
        assertFalse(telephone.isRecycling());
    }

    @Test
    @DisplayName("Should create recycling telephone with automatic timestamps")
    void shouldCreateRecyclingTelephone() {
        Telephone telephone = new Telephone(telephoneId, number, recyclingBusinessId);

        assertEquals(recyclingBusinessId, telephone.getRecyclingBusinessId());
        assertNull(telephone.getUserId());
        assertNull(telephone.getOrganizationId());
        assertNull(telephone.getUnitId());
        assertTrue(telephone.isRecycling());
    }

    @Test
    @DisplayName("Should reconstitute full telephone preserving dates")
    void shouldReconstituteFullTelephone() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 10, 0);

        Telephone telephone = new Telephone(telephoneId, number, userId, organizationId, unitId,
                recyclingBusinessId, createdAt, updatedAt);

        assertEquals(createdAt, telephone.getCreatedAt());
        assertEquals(updatedAt, telephone.getUpdatedAt());
    }

    @Test
    @DisplayName("Should reconstitute user telephone with the 7-arg constructor")
    void shouldReconstituteUserTelephone() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 10, 0);

        Telephone telephone = new Telephone(telephoneId, number, userId, organizationId, unitId,
                createdAt, updatedAt);

        assertEquals(userId, telephone.getUserId());
        assertEquals(createdAt, telephone.getCreatedAt());
        assertEquals(updatedAt, telephone.getUpdatedAt());
    }

    @Test
    @DisplayName("Should reconstitute recycling telephone with the 5-arg constructor")
    void shouldReconstituteRecyclingTelephone() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 10, 0);

        Telephone telephone = new Telephone(telephoneId, number, recyclingBusinessId, createdAt, updatedAt);

        assertTrue(telephone.isRecycling());
        assertEquals(createdAt, telephone.getCreatedAt());
    }

    @Test
    @DisplayName("Should change number and touch updatedAt")
    void shouldChangeNumber() {
        Telephone telephone = new Telephone(telephoneId, number, userId, organizationId, unitId,
                LocalDateTime.of(2024, 1, 1, 10, 0), LocalDateTime.of(2024, 1, 1, 10, 0));
        LocalDateTime before = telephone.getUpdatedAt();

        TelephoneNumber newNumber = new TelephoneNumber("1133334444");
        telephone.changeNumber(newNumber);

        assertEquals(newNumber, telephone.getNumber());
        assertFalse(telephone.getUpdatedAt().isBefore(before));
    }

    @Test
    @DisplayName("Should update updatedAt when calling touch")
    void shouldTouch() {
        Telephone telephone = new Telephone(telephoneId, number, recyclingBusinessId,
                LocalDateTime.of(2024, 1, 1, 10, 0), LocalDateTime.of(2024, 1, 1, 10, 0));
        LocalDateTime before = telephone.getUpdatedAt();

        telephone.touch();

        assertFalse(telephone.getUpdatedAt().isBefore(before));
    }
}
