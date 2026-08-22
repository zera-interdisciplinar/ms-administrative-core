package com.zera.ms_administrative_core.core.domain.entity;

import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;

import java.time.LocalDateTime;
import java.util.UUID;

public class Telephone {
    private final UUID telephoneId;
    private TelephoneNumber number;
    private UUID userId;
    private UUID organizationId;
    private UUID unitId;
    private UUID recyclingBusinessId;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructors
    // Users
    public Telephone(UUID telephoneId, TelephoneNumber number, UUID userId, UUID organizationId, UUID unitId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.telephoneId = telephoneId;
        this.number = number;
        this.userId = userId;
        this.organizationId = organizationId;
        this.unitId = unitId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Telephone(UUID telephoneId, TelephoneNumber number, UUID userId, UUID organizationId, UUID unitId) {
        this.telephoneId = telephoneId;
        this.number = number;
        this.userId = userId;
        this.organizationId = organizationId;
        this.unitId = unitId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Recycling Business

    public Telephone(UUID telephoneId, TelephoneNumber number, UUID recyclingBusinessId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.telephoneId = telephoneId;
        this.number = number;
        this.recyclingBusinessId = recyclingBusinessId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Telephone(UUID telephoneId, TelephoneNumber number, UUID recyclingBusinessId) {
        this.telephoneId = telephoneId;
        this.number = number;
        this.recyclingBusinessId = recyclingBusinessId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters

    public UUID getTelephoneId() {
        return telephoneId;
    }

    public TelephoneNumber getNumber() {
        return number;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getUnitId() {
        return unitId;
    }

    public UUID getRecyclingBusinessId() {
        return recyclingBusinessId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // -----------------------------------------

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void changeNumber(TelephoneNumber newNumber) {
        this.number = newNumber;
        touch();
    }
}
