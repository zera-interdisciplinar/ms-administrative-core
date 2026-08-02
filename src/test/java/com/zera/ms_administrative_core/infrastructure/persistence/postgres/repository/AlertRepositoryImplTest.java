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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
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
                UUID.randomUUID(), now, now, unitId, AlertStatus.OPEN, id);

        AlertJpa savedJpa = mapper.toJpa(alert);
        when(jpa.save(any(AlertJpa.class))).thenReturn(savedJpa);

        Alert result = repository.save(alert);

        assertEquals(id, result.getId());
        assertEquals(AlertStatus.OPEN, result.getStatus());
        verify(jpa).save(any(AlertJpa.class));
    }
}
