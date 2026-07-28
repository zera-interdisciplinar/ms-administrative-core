package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;

public class Alert {
    private final UUID id;
    private final UUID eventId;
    private final UUID ruleId;
    private final UUID userId;
    private final UUID unitId;
    private AlertStatus status;
    private final UUID priorityId;
    private final UUID kindId;
    private String source;
    private String reason;
    private String notes;
    private LocalDateTime occurredAt;
    private LocalDateTime updatedAt;

    public Alert(UUID id, UUID eventId, UUID ruleId, UUID userId, UUID unitId, UUID priorityId, UUID kindId,
            AlertStatus status, String source, String reason, String notes) {
        this.id = id;
        this.eventId = eventId;
        this.ruleId = ruleId;
        this.userId = userId;
        this.unitId = unitId;
        this.priorityId = priorityId;
        this.kindId = kindId;
        this.status = status;
        this.source = source;
        this.reason = reason;
        this.notes = notes;
    }

    public Alert(UUID id, UUID eventId, UUID ruleId, UUID userId, UUID unitId, UUID priorityId, UUID kindId,
            AlertStatus status, String source, String reason, String notes, LocalDateTime occurredAt,
            LocalDateTime updatedAt) {
        this.id = id;
        this.eventId = eventId;
        this.ruleId = ruleId;
        this.userId = userId;
        this.unitId = unitId;
        this.priorityId = priorityId;
        this.kindId = kindId;
        this.status = status;
        this.source = source;
        this.reason = reason;
        this.notes = notes;
        this.occurredAt = occurredAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
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

    public UUID getUnitId() {
        return unitId;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public UUID getPriorityId() {
        return priorityId;
    }

    public UUID getKindId() {
        return kindId;
    }

    public String getSource() {
        return source;
    }

    public String getReason() {
        return reason;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    // ----------------------------------------------------

    public void updateStatus(AlertStatus newStatus){
        this.status = newStatus;
    }
}
