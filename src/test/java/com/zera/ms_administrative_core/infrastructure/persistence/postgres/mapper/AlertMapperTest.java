package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.AlertJpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AlertMapperTest {

    private final AlertMapper mapper = new AlertMapper();

    @Test
    @DisplayName("Deve mapear AlertJpa para Alert de domínio")
    void shouldMapJpaToDomain() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.of(2024, 1, 1, 8, 0);
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 8, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 1, 9, 0);

        AlertJpa jpa = new AlertJpa(id, AlertStatus.OPEN, unitId, eventId, ruleId, userId, "desc", Severity.HIGH,
                AlertKind.STORAGE.name(), occurredAt, createdAt, updatedAt);

        Alert domain = mapper.toDomain(jpa);

        assertEquals(id, domain.getId());
        assertEquals(AlertStatus.OPEN, domain.getStatus());
        assertEquals(unitId, domain.getUnitId());
        assertEquals(eventId, domain.getEventId());
        assertEquals(ruleId, domain.getRuleId());
        assertEquals(userId, domain.getUserId());
        assertEquals("desc", domain.getDescription());
        assertEquals(Severity.HIGH, domain.getSeverity());
        assertEquals(AlertKind.STORAGE, domain.getKind());
        assertEquals(occurredAt, domain.getOccurredAt());
        assertEquals(createdAt, domain.getCreatedAt());
        assertEquals(updatedAt, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("Deve mapear Alert de domínio para AlertJpa")
    void shouldMapDomainToJpa() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime occurredAt = LocalDateTime.of(2024, 1, 1, 8, 0);
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 8, 30);
        LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 1, 9, 0);

        Alert domain = new Alert(AlertKind.TIME, Severity.MEDIUM, "desc", userId, ruleId, eventId, createdAt,
                updatedAt, occurredAt, unitId, AlertStatus.CLOSED, id);

        AlertJpa jpa = mapper.toJpa(domain);

        assertEquals(id, jpa.getId());
        assertEquals(AlertStatus.CLOSED, jpa.getStatus());
        assertEquals(unitId, jpa.getUnitId());
        assertEquals(eventId, jpa.getEventId());
        assertEquals(ruleId, jpa.getRuleId());
        assertEquals(userId, jpa.getUserId());
        assertEquals("desc", jpa.getDescription());
        assertEquals(Severity.MEDIUM, jpa.getSeverity());
        assertEquals(AlertKind.TIME.name(), jpa.getKind());
        assertEquals(occurredAt, jpa.getOccurredAt());
        assertEquals(createdAt, jpa.getCreatedAt());
        assertEquals(updatedAt, jpa.getUpdatedAt());
    }
}
