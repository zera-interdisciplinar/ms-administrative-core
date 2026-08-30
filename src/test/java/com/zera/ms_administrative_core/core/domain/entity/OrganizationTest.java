package com.zera.ms_administrative_core.core.domain.entity;

import com.zera.ms_administrative_core.core.domain.exception.InvalidStatusTransitionException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationTest {

    private final UUID id = UUID.randomUUID();
    private final Cnpj cnpj = new Cnpj("11.222.333/0001-81");
    private final Email email = new Email("org@email.com");

    private Organization organization;

    @BeforeEach
    void setUp() {
        organization = new Organization(id, "Org", cnpj, Status.ACTIVE, email, Plan.FREE);
    }

    @Test
    @DisplayName("Should create organization with correct data and automatic timestamps")
    void shouldCreateWithCorrectData() {
        assertEquals(id, organization.getOrganizationId());
        assertEquals("Org", organization.getName());
        assertEquals(cnpj, organization.getCnpj());
        assertEquals(Status.ACTIVE, organization.getStatus());
        assertEquals(email, organization.getEmail());
        assertEquals(Plan.FREE, organization.getPlan());
        assertNotNull(organization.getCreatedAt());
        assertNotNull(organization.getUpdatedAt());
    }

    @Test
    @DisplayName("Should preserve dates when using reconstitution constructor")
    void shouldPreserveDatesOnReconstitution() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 6, 1, 10, 0);

        Organization reconstituted = new Organization(
                id, "Org", cnpj, Status.ACTIVE, email, Plan.PRO, createdAt, updatedAt);

        assertEquals(createdAt, reconstituted.getCreatedAt());
        assertEquals(updatedAt, reconstituted.getUpdatedAt());
    }

    @Test
    @DisplayName("Should rename and touch updatedAt")
    void shouldRename() {
        LocalDateTime before = organization.getUpdatedAt();
        organization.rename("New Org");
        assertEquals("New Org", organization.getName());
        assertFalse(organization.getUpdatedAt().isBefore(before));
    }

    @Test
    @DisplayName("Should change email and touch updatedAt")
    void shouldChangeEmail() {
        Email newEmail = new Email("new@email.com");
        organization.changeEmail(newEmail);
        assertEquals(newEmail, organization.getEmail());
    }

    @Test
    @DisplayName("Should change plan and touch updatedAt")
    void shouldChangePlan() {
        organization.changePlan(Plan.PRO);
        assertEquals(Plan.PRO, organization.getPlan());
    }

    @Test
    @DisplayName("Should update updatedAt when calling touch")
    void shouldTouch() {
        LocalDateTime before = organization.getUpdatedAt();
        organization.touch();
        assertFalse(organization.getUpdatedAt().isBefore(before));
    }

    // --- status transitions ---

    @Test
    @DisplayName("Should deactivate an active organization")
    void shouldDeactivate() {
        organization.deactivate();
        assertEquals(Status.INACTIVE, organization.getStatus());
    }

    @Test
    @DisplayName("Should be idempotent when deactivating an already inactive organization")
    void shouldIgnoreDeactivateWhenAlreadyInactive() {
        organization.deactivate();
        organization.deactivate();
        assertEquals(Status.INACTIVE, organization.getStatus());
    }

    @Test
    @DisplayName("Should suspend an active organization")
    void shouldSuspend() {
        organization.suspend();
        assertEquals(Status.SUSPENDED, organization.getStatus());
    }

    @Test
    @DisplayName("Should be idempotent when suspending an already suspended organization")
    void shouldIgnoreSuspendWhenAlreadySuspended() {
        organization.suspend();
        organization.suspend();
        assertEquals(Status.SUSPENDED, organization.getStatus());
    }

    @Test
    @DisplayName("Should activate a suspended organization")
    void shouldActivate() {
        organization.suspend();
        organization.activate();
        assertEquals(Status.ACTIVE, organization.getStatus());
    }

    @Test
    @DisplayName("Should reject activating an already active organization")
    void shouldRejectActivateWhenAlreadyActive() {
        assertThrows(InvalidStatusTransitionException.class, () -> organization.activate());
    }

    @Test
    @DisplayName("Should reject suspend from inactive (invalid transition)")
    void shouldRejectSuspendFromInactive() {
        organization.deactivate();
        assertThrows(InvalidStatusTransitionException.class, () -> organization.suspend());
    }
}
