package com.zera.ms_administrative_core.core.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.valueobject.InvitationStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvitationTest {

    @Test
    @DisplayName("Deve criar um convite pendente com código de 6 dígitos")
    void shouldCreatePendingInvitation() {
        UUID managerId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();

        Invitation invitation = new Invitation(UUID.randomUUID(), "123456", managerId, unitId,
                LocalDateTime.now().plusHours(1));

        assertEquals("123456", invitation.getCode());
        assertEquals(managerId, invitation.getManagerId());
        assertEquals(unitId, invitation.getUnitId());
        assertEquals(InvitationStatus.PENDING, invitation.getStatus());
        assertNull(invitation.getUsedByUserId());
        assertFalse(invitation.isExpired());
    }

    @Test
    @DisplayName("Deve rejeitar código que não tenha exatamente 6 dígitos")
    void shouldRejectInvalidCode() {
        assertThrows(IllegalArgumentException.class,
                () -> new Invitation(UUID.randomUUID(), "12345", UUID.randomUUID(), UUID.randomUUID(),
                        LocalDateTime.now().plusHours(1)));
        assertThrows(IllegalArgumentException.class,
                () -> new Invitation(UUID.randomUUID(), "abcdef", UUID.randomUUID(), UUID.randomUUID(),
                        LocalDateTime.now().plusHours(1)));
    }

    @Test
    @DisplayName("Deve considerar expirado quando expiresAt já passou")
    void shouldBeExpiredWhenPast() {
        Invitation invitation = new Invitation(UUID.randomUUID(), "654321", UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.now().minusMinutes(1));

        assertTrue(invitation.isExpired());
    }

    @Test
    @DisplayName("Deve marcar convite como usado e atualizar updatedAt")
    void shouldMarkAsUsed() {
        LocalDateTime before = LocalDateTime.now().minusDays(1);
        Invitation invitation = new Invitation(UUID.randomUUID(), "111222", UUID.randomUUID(), UUID.randomUUID(),
                InvitationStatus.PENDING, LocalDateTime.now().plusHours(1), null, before, before);
        UUID userId = UUID.randomUUID();

        invitation.markUsed(userId);

        assertEquals(InvitationStatus.USED, invitation.getStatus());
        assertEquals(userId, invitation.getUsedByUserId());
        assertNotEquals(before, invitation.getUpdatedAt());
    }
}
