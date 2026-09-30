package com.zera.ms_administrative_core.core.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;

public interface AlertRepository {
    Alert save(Alert alert);
    Optional<Alert> findOpenByRuleIdAndEventId(UUID ruleId, UUID eventId);
    /** {@code status} nulo nao restringe; mais recente primeiro. */
    List<Alert> findByUserId(UUID userId, AlertStatus status, int page, int size);
}
