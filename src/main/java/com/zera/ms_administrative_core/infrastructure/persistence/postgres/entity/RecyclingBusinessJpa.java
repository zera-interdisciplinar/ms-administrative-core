package com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recycling_business")
@Getter
@NoArgsConstructor
public class RecyclingBusinessJpa {
    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String cnpj;

    @Column(nullable = false, name = "contact_email")
    private String contactEmail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public RecyclingBusinessJpa(UUID id, String name, String cnpj, String contactEmail,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.name = name;
        this.cnpj = cnpj;
        this.contactEmail = contactEmail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
