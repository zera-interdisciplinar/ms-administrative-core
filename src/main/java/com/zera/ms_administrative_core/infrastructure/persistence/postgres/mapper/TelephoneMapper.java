package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.TelephoneJpa;

public class TelephoneMapper {
    public Telephone toDomain(TelephoneJpa jpa){
        return new Telephone(
                jpa.getId(),
                new TelephoneNumber(jpa.getNumber()),
                jpa.getUserId(),
                jpa.getOrganizationId(),
                jpa.getUnitId(),
                jpa.getRecyclingBusinessId(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }

    public TelephoneJpa toJpa(Telephone domain) {
        return new TelephoneJpa(
                domain.getTelephoneId(),
                domain.getNumber().toString(),
                domain.getUserId(),
                domain.getOrganizationId(),
                domain.getUnitId(),
                domain.getRecyclingBusinessId(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
