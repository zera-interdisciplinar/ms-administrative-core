package com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "telephone")
@Getter
@NoArgsConstructor
public class TelephoneJpa {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String number;

    @Column(columnDefinition = "uuid", name = "user_id")
    private UUID userId;

    @Column(columnDefinition = "uuid", name="organization_id")
    private UUID organizationId;

    @Column(columnDefinition = "uuid", name = "unit_id")
    private UUID unitId;

    @Column(columnDefinition = "uuid", name = "recycling_business_id")
    private UUID recyclingBusinessId;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;

    public TelephoneJpa(UUID id, String number, UUID userId, UUID organizationId, UUID unitId, UUID recyclingBusinessId, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.number = number;
        this.userId = userId;
        this.organizationId = organizationId;
        this.unitId = unitId;
        this.recyclingBusinessId = recyclingBusinessId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
