package com.zera.ms_administrative_core.core.usecase.notification.closeAlert;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.exception.AlertNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.repository.AlertRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CloseAlertForUserImplTest {

    @Mock
    private AlertRepository repository;

    private CloseAlertForUserImpl useCase() {
        return new CloseAlertForUserImpl(repository);
    }

    private Alert alerta(UUID userId, UUID alertId, AlertStatus status) {
        LocalDateTime agora = LocalDateTime.of(2024, 1, 1, 8, 0);
        return new Alert(AlertKind.WARRANTY_EXPIRATION, Severity.MEDIUM, "Garantia vencendo: Notebook",
                userId, UUID.randomUUID(), UUID.randomUUID(), agora, agora, agora, UUID.randomUUID(),
                status, alertId);
    }

    @Test
    @DisplayName("Deve fechar o alerta OPEN do proprio usuario")
    void shouldCloseOwnOpenAlert() {
        UUID userId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        Alert aberto = alerta(userId, alertId, AlertStatus.OPEN);
        when(repository.findByIdAndUserId(alertId, userId)).thenReturn(Optional.of(aberto));
        when(repository.save(aberto)).thenReturn(aberto);

        var output = useCase().execute(userId, alertId);

        assertEquals(AlertStatus.CLOSED, output.status());
        verify(repository).save(aberto);
    }

    @Test
    @DisplayName("Alerta ja CLOSED nao grava de novo")
    void shouldBeIdempotentWhenAlreadyClosed() {
        UUID userId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        Alert fechado = alerta(userId, alertId, AlertStatus.CLOSED);
        when(repository.findByIdAndUserId(alertId, userId)).thenReturn(Optional.of(fechado));

        var output = useCase().execute(userId, alertId);

        assertEquals(AlertStatus.CLOSED, output.status());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Alerta de outro usuario e 404")
    void shouldHideAlertsThatDoNotBelongToTheUser() {
        UUID userId = UUID.randomUUID();
        UUID alertId = UUID.randomUUID();
        when(repository.findByIdAndUserId(alertId, userId)).thenReturn(Optional.empty());

        assertThrows(AlertNotFoundException.class, () -> useCase().execute(userId, alertId));
        verify(repository, never()).save(any());
    }
}
