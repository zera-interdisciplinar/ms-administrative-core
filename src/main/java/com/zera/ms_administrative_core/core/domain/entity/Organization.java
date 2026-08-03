package com.zera.ms_administrative_core.core.domain.entity;

import com.zera.ms_administrative_core.core.domain.exception.InvalidStatusTransitionException;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;

import java.time.LocalDateTime;
import java.util.UUID;

public class Organization {
    private UUID organizationId;
    private String name;
    private Cnpj cnpj;
    private Status status;
    private Email email;
    private UUID planId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ------------------------------------------
    public Organization(UUID organizationId, String name, Cnpj cnpj, Status status, Email email, UUID planId) {
        this.organizationId = organizationId;
        this.name = name;
        this.cnpj = cnpj;
        this.status = status;
        this.email = email;
        this.planId = planId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    public Organization(UUID organizationId, String name, Cnpj cnpj, Status status, Email email, UUID planId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.organizationId = organizationId;
        this.name = name;
        this.cnpj = cnpj;
        this.status = status;
        this.email = email;
        this.planId = planId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ------------------------------------------

    public UUID getOrganizationId() { return organizationId; }

    public String getName() { return name; }

    public Cnpj getCnpj() { return cnpj; }

    public Status getStatus() { return status; }

    public Email getEmail() { return email; }

    public UUID getPlanId() { return planId; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // ------------------------------------------

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    public void rename(String newName) {
        this.name = newName;
        touch();
    }

    public void changeEmail(Email newEmail) {
        this.email = newEmail;
        touch();
    }

    public void changePlan(UUID newPlanId) {
        this.planId = newPlanId;
        touch();
    }

    public void activate() {
        if (!this.status.canTransitionTo(Status.ACTIVE)) {
            throw new InvalidStatusTransitionException(this.status, Status.ACTIVE);
        }
        this.status = Status.ACTIVE;
        touch();
    }

    public void deactivate() {
        if (this.status == Status.INACTIVE) {
            return;
        }
        if (!this.status.canTransitionTo(Status.INACTIVE)) {
            throw new InvalidStatusTransitionException(this.status, Status.INACTIVE);
        }
        this.status = Status.INACTIVE;
        touch();
    }

    public void suspend() {
        if (this.status == Status.SUSPENDED) {
            return;
        }
        if (!this.status.canTransitionTo(Status.SUSPENDED)) {
            throw new InvalidStatusTransitionException(this.status, Status.SUSPENDED);
        }
        this.status = Status.SUSPENDED;
        touch();
    }
}
