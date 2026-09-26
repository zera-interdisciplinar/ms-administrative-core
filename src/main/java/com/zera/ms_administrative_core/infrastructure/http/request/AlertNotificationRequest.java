package com.zera.ms_administrative_core.infrastructure.http.request;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

/**
 * Alerta ou notificação vindo de outro serviço. {@code eventId} e {@code ruleId} são opcionais,
 * mas quem quiser deduplicação precisa mandar os dois: é por esse par que um alerta repetido é
 * reconhecido em vez de virar um novo.
 *
 * <p>Os demais campos são obrigatórios. Antes eles não eram validados e um corpo incompleto só
 * falhava lá adiante, como 404 ou 500, em vez de 400.
 */
public record AlertNotificationRequest(
        UUID eventId,
        UUID ruleId,
        @NotNull UUID userId,
        @NotNull UUID unitId,
        @NotBlank @Size(max = 500) String description,
        @NotNull Severity severity,
        @NotNull AlertKind kind,
        @NotNull AlertStatus status,
        @PastOrPresent LocalDateTime occurredAt) {

    public NotifyUserCommand toCommand() {
        return new NotifyUserCommand(eventId, ruleId, userId, unitId, description, severity, kind, status,
                occurredAt);
    }
}
