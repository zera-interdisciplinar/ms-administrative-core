package com.zera.ms_administrative_core.core.usecase.notification.closeAlert;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.domain.exception.AlertNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.repository.AlertRepository;
import com.zera.ms_administrative_core.core.usecase.notification.findAlerts.AlertOutput;

/**
 * Fecha o alerta a pedido do dono. Nao ha status READ: OPEN/CLOSED e o que o job e a deduplicacao
 * ja entendem. Alerta de outro usuario some como 404 para nao vazar existencia.
 */
@Service
public class CloseAlertForUserImpl implements CloseAlertForUser {

    private final AlertRepository repository;

    public CloseAlertForUserImpl(AlertRepository repository) {
        this.repository = repository;
    }

    @Override
    public AlertOutput execute(UUID userId, UUID alertId) {
        Alert alert = repository.findByIdAndUserId(alertId, userId)
                .orElseThrow(() -> new AlertNotFoundException(alertId));
        if (alert.getStatus() != AlertStatus.CLOSED) {
            alert.updateStatus(AlertStatus.CLOSED);
            alert = repository.save(alert);
        }
        return AlertOutput.from(alert);
    }
}
