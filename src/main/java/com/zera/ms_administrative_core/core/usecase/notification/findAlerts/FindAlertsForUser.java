package com.zera.ms_administrative_core.core.usecase.notification.findAlerts;

import java.util.List;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;

public interface FindAlertsForUser {
    /** {@code status} nulo traz todos; mais recente primeiro. */
    List<AlertOutput> execute(UUID userId, AlertStatus status, int page, int size);
}
