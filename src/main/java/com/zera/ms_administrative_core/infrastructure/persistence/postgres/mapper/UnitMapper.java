package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UnitJpa;
import org.springframework.stereotype.Component;

@Component
public class UnitMapper {

    public Unit toDomain(UnitJpa jpa) {
        return new Unit(
                jpa.getId(),
                jpa.getName(),
                jpa.getOrganizationId(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }

    public UnitJpa toJpa(Unit domain) {
        return new UnitJpa(
                domain.getUnitId(),
                domain.getName(),
                domain.getOrganizationId(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
