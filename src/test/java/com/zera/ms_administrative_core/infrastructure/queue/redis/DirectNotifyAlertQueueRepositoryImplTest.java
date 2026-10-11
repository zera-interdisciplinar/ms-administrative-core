package com.zera.ms_administrative_core.infrastructure.queue.redis;

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
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUser;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

import static org.mockito.Mockito.verify;

/** Fallback usado com a fila desligada: precisa continuar chamando o NotifyUser direto, sincrono. */
@ExtendWith(MockitoExtension.class)
class DirectNotifyAlertQueueRepositoryImplTest {

    @Mock
    private NotifyUser notifyUser;

    @Test
    @DisplayName("Deve chamar o NotifyUser direto, sem fila")
    void shouldCallNotifyUserDirectly() {
        NotifyUserCommand command = new NotifyUserCommand(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "storage almost full", Severity.HIGH, AlertKind.STORAGE, AlertStatus.OPEN,
                LocalDateTime.now());

        new DirectNotifyAlertQueueRepositoryImpl(notifyUser).enqueue(command);

        verify(notifyUser).execute(command);
    }
}
