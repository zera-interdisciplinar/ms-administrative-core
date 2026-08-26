package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.domain.entity.Invitation;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.InvitationJpa;

@Component
public class InvitationMapper {

    public Invitation toDomain(InvitationJpa jpa) {
        return new Invitation(
                jpa.getId(),
                jpa.getCode(),
                jpa.getManagerId(),
                jpa.getUnitId(),
                jpa.getStatus(),
                jpa.getExpiresAt(),
                jpa.getUsedByUserId(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }

    public InvitationJpa toJpa(Invitation domain) {
        return new InvitationJpa(
                domain.getId(),
                domain.getCode(),
                domain.getManagerId(),
                domain.getUnitId(),
                domain.getStatus(),
                domain.getExpiresAt(),
                domain.getUsedByUserId(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
