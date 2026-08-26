package com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.valueobject.InvitationStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "invitation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvitationJpa {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(name = "manager_id", columnDefinition = "uuid", nullable = false)
    private UUID managerId;

    @Column(name = "unit_id", columnDefinition = "uuid", nullable = false)
    private UUID unitId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private InvitationStatus status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_by_user_id", columnDefinition = "uuid")
    private UUID usedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public InvitationJpa(UUID id, String code, UUID managerId, UUID unitId, InvitationStatus status,
            LocalDateTime expiresAt, UUID usedByUserId, LocalDateTime createdAt, LocalDateTime updatedAt) {
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
}
