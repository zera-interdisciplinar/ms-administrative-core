package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import com.zera.ms_administrative_core.core.domain.entity.Unit;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.UnitJpa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnitMapperTest {

    private final UnitMapper mapper = new UnitMapper();

    private final UUID id = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.of(2024, 1, 1, 10, 0);

    @Test
    @DisplayName("Should map JPA entity to domain entity")
    void shouldMapJpaToDomain() {
        UnitJpa jpa = new UnitJpa(id, "Matriz", organizationId, now, now);

        Unit domain = mapper.toDomain(jpa);

        assertEquals(id, domain.getUnitId());
        assertEquals("Matriz", domain.getName());
        assertEquals(organizationId, domain.getOrganizationId());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("Should map domain entity to JPA entity")
    void shouldMapDomainToJpa() {
        Unit domain = new Unit(id, "Matriz", organizationId, now, now);

        UnitJpa jpa = mapper.toJpa(domain);

        assertEquals(id, jpa.getId());
        assertEquals("Matriz", jpa.getName());
        assertEquals(organizationId, jpa.getOrganizationId());
        assertEquals(now, jpa.getCreatedAt());
        assertEquals(now, jpa.getUpdatedAt());
    }
}
