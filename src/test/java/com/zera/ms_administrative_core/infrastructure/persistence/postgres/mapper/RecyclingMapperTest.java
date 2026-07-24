package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import com.zera.ms_administrative_core.core.domain.entity.RecyclingBusiness;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.RecyclingBusinessJpa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecyclingMapperTest {

    private final RecyclingMapper mapper = new RecyclingMapper();

    @Test
    @DisplayName("Should map JPA entity to domain entity")
    void shouldMapJpaToDomain() {
        UUID id = UUID.randomUUID();
        String name = "Test Company";
        String cnpjStr = "11.222.333/0001-81";
        String emailStr = "test@company.com";
        LocalDateTime now = LocalDateTime.now();

        RecyclingBusinessJpa jpa = new RecyclingBusinessJpa(id, name, cnpjStr, emailStr, now, now);

        RecyclingBusiness domain = mapper.toDomain(jpa);

        assertEquals(id, domain.getId());
        assertEquals(name, domain.getName());
        assertEquals(cnpjStr.replaceAll("\\D", ""), domain.getCnpj().value());
        assertEquals(emailStr, domain.getEmail().value());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("Should map domain entity to JPA entity")
    void shouldMapDomainToJpa() {
        UUID id = UUID.randomUUID();
        String name = "Test Company";
        Cnpj cnpj = new Cnpj("11.222.333/0001-81");
        Email email = new Email("test@company.com");
        LocalDateTime now = LocalDateTime.now();

        RecyclingBusiness domain = new RecyclingBusiness(id, name, cnpj, email, now, now);

        RecyclingBusinessJpa jpa = mapper.toJpa(domain);

        assertEquals(id, jpa.getId());
        assertEquals(name, jpa.getName());
        assertEquals(cnpj.value(), jpa.getCnpj());
        assertEquals(email.value(), jpa.getContactEmail());
        assertEquals(now, jpa.getCreatedAt());
        assertEquals(now, jpa.getUpdatedAt());
    }
}
