package com.zera.ms_administrative_core.core.usecase.findInvitation;

import com.zera.ms_administrative_core.core.domain.entity.Invitation;
import com.zera.ms_administrative_core.core.repository.InvitationRepository;
import com.zera.ms_administrative_core.core.usecase.user.findInvitation.FindPendingInvitationsImpl;
import com.zera.ms_administrative_core.core.usecase.user.findInvitation.PendingInvitationOutput;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindPendingInvitationsImplTest {

    @Mock
    private InvitationRepository invitationRepository;

    @InjectMocks
    private FindPendingInvitationsImpl useCase;

    @Test
    @DisplayName("Deve retornar código, nome do convidado e expiração dos convites pendentes")
    void shouldReturnPendingInvitations() {
        UUID managerId = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(23);
        Invitation invitation = new Invitation(UUID.randomUUID(), "120443", managerId, unitId,
                "Operadora Carol the Best", expiresAt);
        when(invitationRepository.findAllPendingByManager(eq(managerId), any())).thenReturn(List.of(invitation));

        List<PendingInvitationOutput> result = useCase.execute(managerId);

        assertEquals(1, result.size());
        assertEquals("120443", result.get(0).code());
        assertEquals("Operadora Carol the Best", result.get(0).inviteeName());
        assertEquals(expiresAt, result.get(0).expiresAt());
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando o gestor não tem convites pendentes")
    void shouldReturnEmptyListWhenNoPendingInvitations() {
        UUID managerId = UUID.randomUUID();
        when(invitationRepository.findAllPendingByManager(eq(managerId), any())).thenReturn(List.of());

        List<PendingInvitationOutput> result = useCase.execute(managerId);

        assertTrue(result.isEmpty());
    }
}
