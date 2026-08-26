package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.entity.Invitation;
import com.zera.ms_administrative_core.core.domain.valueobject.InvitationStatus;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.InvitationJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.InvitationMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvitationRepositoryImplTest {

    @Mock
    private InvitationJpaRepository jpa;

    private final InvitationMapper mapper = new InvitationMapper();

    private InvitationRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new InvitationRepositoryImpl(jpa, mapper);
    }

    @Test
    @DisplayName("Deve salvar um convite")
    void shouldSaveInvitation() {
        Invitation invitation = new Invitation(UUID.randomUUID(), "123456", UUID.randomUUID(), UUID.randomUUID(),
                LocalDateTime.now().plusHours(1));
        when(jpa.save(any(InvitationJpa.class))).thenReturn(mapper.toJpa(invitation));

        Invitation saved = repository.save(invitation);

        assertEquals(invitation.getCode(), saved.getCode());
        verify(jpa).save(any(InvitationJpa.class));
    }

    @Test
    @DisplayName("Deve encontrar convite pendente por código")
    void shouldFindPendingByCode() {
        InvitationJpa jpaEntity = new InvitationJpa(UUID.randomUUID(), "123456", UUID.randomUUID(),
                UUID.randomUUID(), InvitationStatus.PENDING, LocalDateTime.now().plusHours(1), null,
                LocalDateTime.now(), LocalDateTime.now());
        when(jpa.findByCodeAndStatus("123456", InvitationStatus.PENDING)).thenReturn(Optional.of(jpaEntity));

        Optional<Invitation> result = repository.findPendingByCode("123456");

        assertTrue(result.isPresent());
        assertEquals("123456", result.get().getCode());
    }

    @Test
    @DisplayName("Deve retornar vazio quando não há convite pendente com o código")
    void shouldReturnEmptyWhenNoPendingInvitation() {
        when(jpa.findByCodeAndStatus("000000", InvitationStatus.PENDING)).thenReturn(Optional.empty());

        Optional<Invitation> result = repository.findPendingByCode("000000");

        assertFalse(result.isPresent());
    }
}
