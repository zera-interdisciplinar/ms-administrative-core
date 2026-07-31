package com.zera.ms_administrative_core.infrastructure.http.request;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

public record AlertNotificationRequest(
        UUID eventId,
        UUID ruleId,
        UUID userId,
        UUID unitId,
        String description,
        com.zera.ms_administrative_core.core.domain.entity.Severity severity,
        AlertKind kind,
        AlertStatus status,
        LocalDateTime occurredAt) {
    public NotifyUserCommand toCommand() {
        return new NotifyUserCommand(eventId, ruleId, userId, unitId, description, severity, kind, status, occurredAt);

        // TODO: substituir Integer eventId no diagrama do banco Neo4j
    }
}