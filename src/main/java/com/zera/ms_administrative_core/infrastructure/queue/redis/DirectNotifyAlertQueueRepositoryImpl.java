package com.zera.ms_administrative_core.infrastructure.queue.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.repository.AlertQueueRepository;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUser;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

/**
 * Adaptador padrao quando a fila esta desligada ({@code zera.alerts.queue.enabled=false}, o
 * padrao): chama o {@code NotifyUser} direto, no mesmo request HTTP -- exatamente o comportamento
 * de antes desta fila existir. Mantem a aplicacao funcional sem exigir Redis disponivel.
 */
@Component
@ConditionalOnProperty(name = "zera.alerts.queue.enabled", havingValue = "false", matchIfMissing = true)
class DirectNotifyAlertQueueRepositoryImpl implements AlertQueueRepository {

    private final NotifyUser notifyUser;

    DirectNotifyAlertQueueRepositoryImpl(NotifyUser notifyUser) {
        this.notifyUser = notifyUser;
    }

    @Override
    public void enqueue(NotifyUserCommand command) {
        notifyUser.execute(command);
    }
}
