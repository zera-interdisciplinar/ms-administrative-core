package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import com.zera.ms_administrative_core.core.domain.entity.Organization;
import com.zera.ms_administrative_core.core.domain.entity.Plan;
import com.zera.ms_administrative_core.core.domain.valueobject.Cnpj;
import com.zera.ms_administrative_core.core.domain.valueobject.Email;
import com.zera.ms_administrative_core.core.domain.valueobject.Status;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.OrganizationJpa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrganizationMapperTest {

    private final OrganizationMapper mapper = new OrganizationMapper();

    private final UUID id = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.of(2024, 1, 1, 10, 0);

    @Test
    @DisplayName("Should map JPA entity to domain entity")
    void shouldMapJpaToDomain() {
        OrganizationJpa jpa = new OrganizationJpa(id, "Org", "11222333000181",
                Status.ACTIVE, "org@email.com", Plan.PRO, now, now);

        Organization domain = mapper.toDomain(jpa);

        assertEquals(id, domain.getOrganizationId());
        assertEquals("Org", domain.getName());
        assertEquals("11222333000181", domain.getCnpj().value());
        assertEquals(Status.ACTIVE, domain.getStatus());
        assertEquals("org@email.com", domain.getEmail().value());
        assertEquals(Plan.PRO, domain.getPlan());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("Should map domain entity to JPA entity")
    void shouldMapDomainToJpa() {
        Organization domain = new Organization(id, "Org", new Cnpj("11.222.333/0001-81"),
                Status.ACTIVE, new Email("org@email.com"), Plan.FREE, now, now);

        OrganizationJpa jpa = mapper.toJpa(domain);

        assertEquals(id, jpa.getId());
        assertEquals("Org", jpa.getName());
        assertEquals("11222333000181", jpa.getCnpj());
        assertEquals(Status.ACTIVE, jpa.getStatus());
        assertEquals("org@email.com", jpa.getEmail());
        assertEquals(Plan.FREE, jpa.getPlan());
        assertEquals(now, jpa.getCreatedAt());
        assertEquals(now, jpa.getUpdatedAt());
    }
}
