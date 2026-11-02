package com.zera.ms_administrative_core.core.usecase.notification.closeAlert;

import java.util.UUID;

import com.zera.ms_administrative_core.core.usecase.notification.findAlerts.AlertOutput;

public interface CloseAlertForUser {
    AlertOutput execute(UUID userId, UUID alertId);
}
