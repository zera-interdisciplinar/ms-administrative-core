package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import com.zera.ms_administrative_core.core.domain.entity.Telephone;
import com.zera.ms_administrative_core.core.domain.valueobject.TelephoneNumber;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.TelephoneJpa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelephoneMapperTest {

    private final TelephoneMapper mapper = new TelephoneMapper();

    private final UUID id = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID organizationId = UUID.randomUUID();
    private final UUID unitId = UUID.randomUUID();
    private final UUID recyclingId = UUID.randomUUID();
    private final LocalDateTime now = LocalDateTime.of(2024, 1, 1, 10, 0);

    @Test
    @DisplayName("Should map JPA entity to domain entity")
    void shouldMapJpaToDomain() {
        TelephoneJpa jpa = new TelephoneJpa(id, "11987654321", userId, organizationId, unitId,
                recyclingId, now, now);

        Telephone domain = mapper.toDomain(jpa);

        assertEquals(id, domain.getTelephoneId());
        assertEquals("11987654321", domain.getNumber().value());
        assertEquals(userId, domain.getUserId());
        assertEquals(organizationId, domain.getOrganizationId());
        assertEquals(unitId, domain.getUnitId());
        assertEquals(recyclingId, domain.getRecyclingBusinessId());
        assertEquals(now, domain.getCreatedAt());
        assertEquals(now, domain.getUpdatedAt());
    }

    @Test
    @DisplayName("Should map domain entity to JPA entity")
    void shouldMapDomainToJpa() {
        Telephone domain = new Telephone(id, new TelephoneNumber("11987654321"), userId,
                organizationId, unitId, recyclingId, now, now);

        TelephoneJpa jpa = mapper.toJpa(domain);

        assertEquals(id, jpa.getId());
        assertEquals("11987654321", jpa.getNumber());
        assertEquals(userId, jpa.getUserId());
        assertEquals(organizationId, jpa.getOrganizationId());
        assertEquals(unitId, jpa.getUnitId());
        assertEquals(recyclingId, jpa.getRecyclingBusinessId());
        assertEquals(now, jpa.getCreatedAt());
        assertEquals(now, jpa.getUpdatedAt());
    }
}
