package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public class Unit {
    private final UUID unitId;
    private String name;
    private final UUID organizationId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Unit(UUID unitId, String name, UUID organizationId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.unitId = unitId;
        this.name = name;
        this.organizationId = organizationId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Unit(UUID unitId, String name, UUID organizationId) {
        this.unitId = unitId;
        this.name = name;
        this.organizationId = organizationId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getUnitId() {
        return unitId;
    }

    public String getName() {
        return name;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void rename(String newName) {
        this.name = newName;
        touch();
    }
}
