package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.RecyclingBusinessJpa;
import org.springframework.stereotype.Component;

@Component
public class RecyclingMapper {

    public RecyclingBusiness toDomain(RecyclingBusinessJpa jpa) {
        return new RecyclingBusiness(
                jpa.getId(),
                jpa.getName(),
                new Cnpj(jpa.getCnpj()),
                new Email(jpa.getContactEmail()),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }

    public RecyclingBusinessJpa toJpa(RecyclingBusiness domain) {
        return new RecyclingBusinessJpa(
                domain.getId(),
                domain.getName(),
                domain.getCnpj().value(),
                domain.getEmail().value(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }
}
