package com.zera.ms_administrative_core.infrastructure.persistence.postgres.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.entity.AlertJpa;
import com.zera.ms_administrative_core.infrastructure.persistence.postgres.mapper.AlertMapper;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertRepositoryImplTest {

    @Mock
    private AlertJpaRepository jpa;

    private final AlertMapper mapper = new AlertMapper();

    private AlertRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        repository = new AlertRepositoryImpl(jpa, mapper);
    }

    @Test
    @DisplayName("Deve salvar um alerta e retornar o alerta de domínio mapeado")
    void shouldSaveAndReturnMappedAlert() {
        UUID id = UUID.randomUUID();
        UUID unitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 8, 0);

        Alert alert = new Alert(AlertKind.STORAGE, Severity.HIGH, "desc", userId, UUID.randomUUID(),
                UUID.randomUUID(), now, now, now, unitId, AlertStatus.OPEN, id);

        AlertJpa savedJpa = mapper.toJpa(alert);
        when(jpa.save(any(AlertJpa.class))).thenReturn(savedJpa);

        Alert result = repository.save(alert);

        assertEquals(id, result.getId());
        assertEquals(AlertStatus.OPEN, result.getStatus());
        verify(jpa).save(any(AlertJpa.class));
    }

    @Test
    @DisplayName("Deve retornar o alerta OPEN existente para o par ruleId/eventId informado")
    void shouldFindOpenAlertByRuleIdAndEventId() {
        UUID ruleId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 8, 0);

        Alert alert = new Alert(AlertKind.STORAGE, Severity.HIGH, "desc", UUID.randomUUID(), ruleId, eventId,
                now, now, now, UUID.randomUUID(), AlertStatus.OPEN, UUID.randomUUID());
        AlertJpa jpaAlert = mapper.toJpa(alert);

        when(jpa.findByRuleIdAndEventIdAndStatus(eq(ruleId), eq(eventId), eq(AlertStatus.OPEN)))
                .thenReturn(Optional.of(jpaAlert));

        Optional<Alert> result = repository.findOpenByRuleIdAndEventId(ruleId, eventId);

        assertTrue(result.isPresent());
        assertEquals(ruleId, result.get().getRuleId());
        assertEquals(eventId, result.get().getEventId());
    }

    @Test
    @DisplayName("Deve listar os alertas do usuario, sem filtro de status")
    void shouldListAlertsByUserWithoutStatusFilter() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 8, 0);

        Alert alert = new Alert(AlertKind.STORAGE, Severity.HIGH, "desc", userId, UUID.randomUUID(),
                UUID.randomUUID(), now, now, now, UUID.randomUUID(), AlertStatus.OPEN, UUID.randomUUID());
        AlertJpa jpaAlert = mapper.toJpa(alert);

        when(jpa.findAllByUserIdAndStatus(eq(userId), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(jpaAlert)));

        List<Alert> result = repository.findByUserId(userId, null, 0, 20);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
    }
}
