package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AlertTest {

    @Test
    @DisplayName("Deve criar um Alert com timestamps explícitos e expor todos os getters")
    void shouldExposeAllFieldsWhenCreatedWithExplicitTimestamps() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 1, 11, 0);

        Alert alert = new Alert(AlertKind.STORAGE, Severity.HIGH, "disk almost full", userId, ruleId, eventId,
                updatedAt, occurredAt, unitId, AlertStatus.OPEN, id);

        assertEquals(id, alert.getId());
        assertEquals(AlertStatus.OPEN, alert.getStatus());
        assertEquals(unitId, alert.getUnitId());
        assertEquals(occurredAt, alert.getOccurredAt());
        assertEquals(updatedAt, alert.getUpdatedAt());
        assertEquals(eventId, alert.getEventId());
        assertEquals(ruleId, alert.getRuleId());
        assertEquals(userId, alert.getUserId());
        assertEquals("disk almost full", alert.getDescription());
        assertEquals(Severity.HIGH, alert.getSeverity());
        assertEquals(AlertKind.STORAGE, alert.getKind());
    }

    @Test
    @DisplayName("Deve criar um Alert com timestamps automáticos quando não informados")
    void shouldGenerateTimestampsWhenNotProvided() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Alert alert = new Alert(AlertKind.TIME, Severity.LOW, "late shift", userId, ruleId, eventId, unitId,
                AlertStatus.OPEN, id);

        assertNotNull(alert.getOccurredAt());
        assertNotNull(alert.getUpdatedAt());
        assertEquals(AlertKind.TIME, alert.getKind());
        assertEquals(Severity.LOW, alert.getSeverity());
    }

    @Test
    @DisplayName("touch deve atualizar apenas o updatedAt")
    void touchShouldUpdateOnlyUpdatedAt() {
        Alert alert = newOpenAlert();
        LocalDateTime before = alert.getUpdatedAt();

        alert.touch();

        assertNotEquals(before, alert.getUpdatedAt());
    }

    @Test
    @DisplayName("updateStatus deve alterar o status e atualizar updatedAt")
    void updateStatusShouldChangeStatusAndTouch() {
        Alert alert = newOpenAlert();
        LocalDateTime before = alert.getUpdatedAt();

        alert.updateStatus(AlertStatus.CLOSED);

        assertEquals(AlertStatus.CLOSED, alert.getStatus());
        assertNotEquals(before, alert.getUpdatedAt());
    }

    @Test
    @DisplayName("escalateSeverity deve alterar a severidade e atualizar updatedAt")
    void escalateSeverityShouldChangeSeverityAndTouch() {
        Alert alert = newOpenAlert();
        LocalDateTime before = alert.getUpdatedAt();

        alert.escalateSeverity(Severity.HIGH);

        assertEquals(Severity.HIGH, alert.getSeverity());
        assertNotEquals(before, alert.getUpdatedAt());
    }

    private Alert newOpenAlert() {
        return new Alert(AlertKind.STORAGE, Severity.LOW, "desc", UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), LocalDateTime.of(2024, 1, 1, 8, 0), LocalDateTime.of(2024, 1, 1, 8, 0),
                UUID.randomUUID(), AlertStatus.OPEN, UUID.randomUUID());
    }
}
