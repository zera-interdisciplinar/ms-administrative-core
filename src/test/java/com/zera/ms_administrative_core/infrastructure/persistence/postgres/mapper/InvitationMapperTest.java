package com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.entity.Invitation;
import com.zera.ms_administrative_core.core.domain.valueobject.InvitationStatus;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.InvitationJpa;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InvitationMapperTest {

    private final InvitationMapper mapper = new InvitationMapper();

    @Test
    @DisplayName("Deve mapear InvitationJpa para Domain Invitation")
    void shouldMapJpaToDomain() {
        UUID id = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        InvitationJpa jpa = new InvitationJpa(id, "123456", managerId, unitId, InvitationStatus.PENDING,
                now.plusHours(1), null, now, now);

        Invitation domain = mapper.toDomain(jpa);

        assertEquals(id, domain.getId());
        assertEquals("123456", domain.getCode());
        assertEquals(managerId, domain.getManagerId());
        assertEquals(unitId, domain.getUnitId());
        assertEquals(InvitationStatus.PENDING, domain.getStatus());
        assertEquals(now.plusHours(1), domain.getExpiresAt());
    }

    @Test
    @DisplayName("Deve mapear Domain Invitation para InvitationJpa")
    void shouldMapDomainToJpa() {
        UUID id = UUID.randomUUID();
        UUID managerId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID usedBy = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();
        Invitation domain = new Invitation(id, "654321", managerId, unitId, InvitationStatus.USED,
                now.plusHours(1), usedBy, now, now);

        InvitationJpa jpa = mapper.toJpa(domain);

        assertEquals(id, jpa.getId());
        assertEquals("654321", jpa.getCode());
        assertEquals(managerId, jpa.getManagerId());
        assertEquals(unitId, jpa.getUnitId());
        assertEquals(InvitationStatus.USED, jpa.getStatus());
        assertEquals(usedBy, jpa.getUsedByUserId());
    }
}
