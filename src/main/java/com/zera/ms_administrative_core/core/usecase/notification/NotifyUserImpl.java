package com.zera.ms_administrative_core.core.usecase.notification;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.repository.AlertRepository;
import com.zera.ms_administrative_core.core.repository.UserRepository;

@Service
public class NotifyUserImpl implements NotifyUser {
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(NotifyUserImpl.class);

    public NotifyUserImpl(AlertRepository alertRepository, UserRepository userRepository) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void execute(NotifyUserCommand command) {
        userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        if (command.ruleId() != null && command.eventId() != null) {
            Optional<Alert> existing = alertRepository.findOpenByRuleIdAndEventId(command.ruleId(), command.eventId());
            if (existing.isPresent()) {
                touchExisting(existing.get(), command);
                return;
            }
        }

        Alert alert = new Alert(command.kind(), command.severity(), command.description(), command.userId(),
                command.ruleId(), command.eventId(), command.unitId(), command.status(), UUID.randomUUID());

        try {
            alertRepository.save(alert);
        } catch (DataIntegrityViolationException raceLost) {
            // outra chamada concorrente criou o alerta primeiro; trata como duplicata
            alertRepository.findOpenByRuleIdAndEventId(command.ruleId(), command.eventId())
                    .ifPresent(winner -> touchExisting(winner, command));
            return;
        }

        log.info("[ALERT] severity={} kind={} userId={} description={} status={}",
                command.severity(),
                command.kind(),
                command.userId(),
                command.description(),
                command.status());

    }

    private void touchExisting(Alert existing, NotifyUserCommand command) {
        if (command.severity().compareTo(existing.getSeverity()) > 0) {
            existing.escalateSeverity(command.severity());
        } else {
            existing.touch();
        }
        alertRepository.save(existing);

        log.info("[ALERT] duplicate suppressed, existing alert refreshed. id={} ruleId={} eventId={}",
                existing.getId(), command.ruleId(), command.eventId());
    }
}
