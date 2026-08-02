package com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity;

import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertJpa {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private AlertStatus status;

    @Column(name = "unit_id", columnDefinition = "uuid", nullable = false)
    private UUID unitId;

    @Column(name = "event_id", columnDefinition = "uuid")
    private UUID eventId;

    @Column(name = "rule_id", columnDefinition = "uuid")
    private UUID ruleId;

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Severity severity;

    @Column(nullable = false, length = 50)
    private String kind;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public AlertJpa(UUID id, AlertStatus status, UUID unitId, UUID eventId,
                    UUID ruleId, UUID userId, String description,
                    Severity severity, String kind, LocalDateTime occurredAt,
                    LocalDateTime updatedAt) {
        this.id = id;
        this.status = status;
        this.unitId = unitId;
        this.eventId = eventId;
        this.ruleId = ruleId;
        this.userId = userId;
        this.description = description;
        this.severity = severity;
        this.kind = kind;
        this.occurredAt = occurredAt;
        this.updatedAt = updatedAt;
    }
}