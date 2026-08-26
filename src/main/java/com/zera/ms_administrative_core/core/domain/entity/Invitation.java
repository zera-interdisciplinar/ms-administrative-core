package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

import com.zera.ms_administrative_core.core.domain.valueobject.InvitationStatus;

public class Invitation {

    private static final Pattern CODE_PATTERN = Pattern.compile("^\\d{6}$");

    private final UUID id;
    private final String code;
    private final UUID managerId;
    private final UUID unitId;
    private InvitationStatus status;
    private final LocalDateTime expiresAt;
    private UUID usedByUserId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Invitation(UUID id, String code, UUID managerId, UUID unitId, LocalDateTime expiresAt) {
        this(id, code, managerId, unitId, InvitationStatus.PENDING, expiresAt, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    // used when loading data from database
    public Invitation(UUID id, String code, UUID managerId, UUID unitId, InvitationStatus status,
            LocalDateTime expiresAt, UUID usedByUserId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException("Invitation code must have exactly 6 digits: " + code);
        }
        this.id = id;
        this.code = code;
        this.managerId = managerId;
        this.unitId = unitId;
        this.status = status;
        this.expiresAt = expiresAt;
        this.usedByUserId = usedByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public UUID getManagerId() {
        return managerId;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public UUID getUsedByUserId() {
        return usedByUserId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // ------------------------------------------

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public void markUsed(UUID userId) {
        this.status = InvitationStatus.USED;
        this.usedByUserId = userId;
        touch();
    }

    private void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
