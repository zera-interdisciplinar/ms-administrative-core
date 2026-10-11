package com.zera.ms_administrative_core.core.usecase.notification.enqueueAlert;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.entity.AlertKind;
import com.zera.ms_administrative_core.core.domain.entity.Severity;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.repository.AlertQueueRepository;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EnqueueAlertNotificationImplTest {

    @Mock
    private AlertQueueRepository alertQueueRepository;

    @Test
    @DisplayName("Deve so delegar o comando para a porta da fila")
    void shouldDelegateToTheQueuePort() {
        NotifyUserCommand command = new NotifyUserCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "storage almost full", Severity.HIGH, AlertKind.STORAGE, AlertStatus.OPEN,
                LocalDateTime.now());

        new EnqueueAlertNotificationImpl(alertQueueRepository).execute(command);

        verify(alertQueueRepository).enqueue(command);
    }
}
