package com.zera.ms_administrative_core.core.repository;

import java.util.Optional;
import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.entity.Alert;

public interface AlertRepository {
    Alert save(Alert alert);
    Optional<Alert> findOpenByRuleIdAndEventId(UUID ruleId, UUID eventId);
}
