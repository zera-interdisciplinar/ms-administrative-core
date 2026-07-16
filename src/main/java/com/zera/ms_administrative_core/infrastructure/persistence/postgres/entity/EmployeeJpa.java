package com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity;

import com.zera.ms_administrative_core.core.domain.entity.Role;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@DiscriminatorValue("EMPLOYEE")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmployeeJpa extends UserJpa {

    public EmployeeJpa(UUID id, String name, String email, String password,
                       Status status, UUID unitId,
                       LocalDateTime createdAt, LocalDateTime updatedAt) {
        super(id, name, email, password, Role.MANAGER, status, unitId, createdAt, updatedAt);
    }
}