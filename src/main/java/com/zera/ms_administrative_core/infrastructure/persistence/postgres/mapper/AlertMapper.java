package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.AlertJpa;

@Component
public class AlertMapper {
    public Alert toDomain(AlertJpa jpa) {
        return new Alert(AlertKind.valueOf(jpa.getKind()), jpa.getSeverity(), jpa.getDescription(), jpa.getUserId(),
                jpa.getRuleId(), jpa.getEventId(), jpa.getUpdatedAt(), jpa.getOccurredAt(), jpa.getUnitId(),
                jpa.getStatus(), jpa.getId());
    }

    public AlertJpa toJpa(Alert domain) {
        return new AlertJpa(domain.getId(), domain.getStatus(), domain.getUnitId(), domain.getEventId(),
                domain.getRuleId(), domain.getUserId(), domain.getDescription(), domain.getSeverity(),
                domain.getKind().name(), domain.getOccurredAt(), domain.getUpdatedAt());
    }
}
