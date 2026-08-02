package com.zera.ms_administrative_core.core.usecase.notification;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;


public record NotifyUserCommand(
    UUID eventId,
    UUID ruleId,
    UUID userId,
    UUID unitId,
    String description,
    Severity severity,
    AlertKind kind,
    AlertStatus status,
    LocalDateTime occurredAt
) {}