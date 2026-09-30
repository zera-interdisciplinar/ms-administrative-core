package com.zera.ms_administrative_core.core.usecase.notification.findAlerts;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.valueobject.AlertStatus;
import com.zera.ms_administrative_core.core.repository.AlertRepository;

@Service
public class FindAlertsForUserImpl implements FindAlertsForUser {

    private final AlertRepository repository;

    public FindAlertsForUserImpl(AlertRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<AlertOutput> execute(UUID userId, AlertStatus status, int page, int size) {
        return repository.findByUserId(userId, status, page, size).stream()
                .map(AlertOutput::from)
                .toList();
    }
}
