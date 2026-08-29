package com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "telephone")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
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
    private UUID recyclingBussinesId;

    @Column(nullable = false, name = "created_at")
    private LocalDateTime createdAt;

    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
