package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.OrganizationJpa;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMapper {

    public Organization toDomain(OrganizationJpa jpa) {
        return new Organization(
                jpa.getId(),
                jpa.getName(),
                new Cnpj(jpa.getCnpj()),
                jpa.getStatus(),
                new Email(jpa.getEmail()),
                jpa.getPlan(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }

    public OrganizationJpa toJpa(Organization domain) {
        return new OrganizationJpa(
                domain.getOrganizationId(),
                domain.getName(),
                domain.getCnpj().value(),
                domain.getStatus(),
                domain.getEmail().value(),
                domain.getPlan(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
