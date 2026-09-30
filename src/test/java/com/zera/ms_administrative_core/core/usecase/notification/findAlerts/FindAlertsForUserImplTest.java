package com.zera.ms_administrative_core.core.usecase.notification.findAlerts;

import java.time.LocalDateTime;
import java.util.List;
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
import com.zera.ms_administrative_core.core.repository.AlertRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindAlertsForUserImplTest {

    @Mock
    private AlertRepository repository;

    private FindAlertsForUserImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindAlertsForUserImpl(repository);
    }

    @Test
    @DisplayName("Deve buscar os alertas do usuario autenticado e mapear para AlertOutput")
    void shouldFindAlertsForUser() {
        UUID userId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2024, 1, 1, 8, 0);
        Alert alert = new Alert(AlertKind.USAGE_INTENSITY_LIMIT, Severity.HIGH, "desc", userId,
                UUID.randomUUID(), UUID.randomUUID(), now, now, now, UUID.randomUUID(), AlertStatus.OPEN,
                UUID.randomUUID());

        when(repository.findByUserId(eq(userId), eq(AlertStatus.OPEN), eq(0), eq(20)))
                .thenReturn(List.of(alert));

        List<AlertOutput> result = useCase.execute(userId, AlertStatus.OPEN, 0, 20);

        assertEquals(1, result.size());
        assertEquals(alert.getId(), result.get(0).alertId());
        assertEquals(alert.getKind(), result.get(0).kind());
        verify(repository).findByUserId(userId, AlertStatus.OPEN, 0, 20);
    }
}
