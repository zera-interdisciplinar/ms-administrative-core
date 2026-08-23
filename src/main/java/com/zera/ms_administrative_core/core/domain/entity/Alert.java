package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;

public class Alert {
    private final UUID id;
    private AlertStatus status;
    private final UUID unitId;
    private final LocalDateTime occurredAt;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private final UUID eventId;
    private final UUID ruleId;
    private final UUID userId;
    private String description;
    private Severity severity;
    private final AlertKind kind;

    public Alert(AlertKind kind, Severity severity, String description, UUID userId, UUID ruleId, UUID eventId,
            LocalDateTime createdAt, LocalDateTime updatedAt, LocalDateTime occurredAt, UUID unitId,
            AlertStatus status, UUID id) {
        this.kind = kind;
        this.severity = severity;
        this.description = description;
        this.userId = userId;
        this.ruleId = ruleId;
        this.eventId = eventId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.occurredAt = occurredAt;
        this.unitId = unitId;
        this.status = status;
        this.id = id;
    }

    public Alert(AlertKind kind, Severity severity, String description, UUID userId, UUID ruleId, UUID eventId,
            UUID unitId, AlertStatus status, UUID id) {
        this.kind = kind;
        this.severity = severity;
        this.description = description;
        this.userId = userId;
        this.ruleId = ruleId;
        this.eventId = eventId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.occurredAt = LocalDateTime.now();
        this.unitId = unitId;
        this.status = status;
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getDescription() {
        return description;
    }

    public Severity getSeverity() {
        return severity;
    }

    public AlertKind getKind() {
        return kind;
    }

    // --------------------------------------

    public void touch(){
        this.updatedAt = LocalDateTime.now();
    }
    
    public void updateStatus(AlertStatus newStatus) {
        touch();
        this.status = newStatus;
    }

    public void escalateSeverity(Severity newSeverity){
        touch();
        this.severity = newSeverity;
    }
}
