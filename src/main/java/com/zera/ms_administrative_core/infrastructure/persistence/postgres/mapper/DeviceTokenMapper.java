package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.domain.entity.DeviceToken;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.DeviceTokenJpa;

@Component
public class DeviceTokenMapper {
    public DeviceToken toDomain(DeviceTokenJpa jpa) {
        return new DeviceToken(jpa.getId(), jpa.getUserId(), jpa.getToken(), jpa.getPlatform(), jpa.getCreatedAt(),
                jpa.getUpdatedAt());
    }

    public DeviceTokenJpa toJpa(DeviceToken domain) {
        return new DeviceTokenJpa(domain.getId(), domain.getUserId(), domain.getToken(), domain.getPlatform(),
                domain.getCreatedAt(), domain.getUpdatedAt());
    }
}
