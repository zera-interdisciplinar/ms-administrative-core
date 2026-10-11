package com.zera.ms_administrative_core.core.repository;

import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

/**
 * Porta para a fila de processamento assincrono de alertas. O POST que recebe o alerta do
 * ms-inventory so enfileira e devolve 202; quem valida o usuario, deduplica e persiste e o worker
 * que consome esta fila, chamando o {@code NotifyUser} de sempre.
 */
public interface AlertQueueRepository {
    void enqueue(NotifyUserCommand command);
}
