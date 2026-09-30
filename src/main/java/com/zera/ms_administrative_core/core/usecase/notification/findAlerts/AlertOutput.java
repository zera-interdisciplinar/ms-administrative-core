package com.zera.ms_administrative_core.core.usecase.notification.findAlerts;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;

public record AlertOutput(
        UUID alertId,
        AlertKind kind,
        Severity severity,
        AlertStatus status,
        String description,
        UUID unitId,
        UUID ruleId,
        UUID eventId,
        LocalDateTime occurredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AlertOutput from(Alert alert) {
        return new AlertOutput(
                alert.getId(),
                alert.getKind(),
                alert.getSeverity(),
                alert.getStatus(),
                alert.getDescription(),
                alert.getUnitId(),
                alert.getRuleId(),
                alert.getEventId(),
                alert.getOccurredAt(),
                alert.getCreatedAt(),
                alert.getUpdatedAt()
        );
    }
}
