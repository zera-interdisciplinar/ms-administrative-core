package com.zera.ms_administrative_core.core.usecase.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.entity.Alert;
import com.zera.ms_administrative_core.core.domain.entity.DeviceToken;
import com.zera.ms_administrative_core.core.domain.exception.UserNotFoundException;
import com.zera.ms_administrative_core.core.domain.valueobject.PushMessage;
import com.zera.ms_administrative_core.core.repository.AlertRepository;
import com.zera.ms_administrative_core.core.repository.DeviceTokenRepository;
import com.zera.ms_administrative_core.core.repository.PushNotificationGateway;
import com.zera.ms_administrative_core.core.repository.UserRepository;

@Service
public class NotifyUserImpl implements NotifyUser {
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final PushNotificationGateway pushNotificationGateway;

    private static final Logger log = LoggerFactory.getLogger(NotifyUserImpl.class);

    public NotifyUserImpl(AlertRepository alertRepository, UserRepository userRepository,
            DeviceTokenRepository deviceTokenRepository, PushNotificationGateway pushNotificationGateway) {
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.deviceTokenRepository = deviceTokenRepository;
        this.pushNotificationGateway = pushNotificationGateway;
    }

    @Override
    public void execute(NotifyUserCommand command) {
        userRepository.findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        if (command.ruleId() != null && command.eventId() != null) {
            Optional<Alert> existing = alertRepository.findOpenByRuleIdAndEventIdAndUserId(command.ruleId(), command.eventId(), command.userId());
            if (existing.isPresent()) {
                touchExisting(existing.get(), command);
                return;
            }
        }

        // o momento informado por quem detectou vale mais que a hora em que a chamada chegou aqui;
        // sem ele, cai no agora
        LocalDateTime occurredAt = command.occurredAt() != null ? command.occurredAt() : LocalDateTime.now();
        Alert alert = new Alert(command.kind(), command.severity(), command.description(), command.userId(),
                command.ruleId(), command.eventId(), LocalDateTime.now(), LocalDateTime.now(), occurredAt,
                command.unitId(), command.status(), UUID.randomUUID());

        try {
            alertRepository.save(alert);
        } catch (DataIntegrityViolationException raceLost) {
            // outra chamada concorrente criou o alerta primeiro; trata como duplicata
            alertRepository.findOpenByRuleIdAndEventIdAndUserId(command.ruleId(), command.eventId(), command.userId())
                    .ifPresent(winner -> touchExisting(winner, command));
            return;
        }

        log.info("[ALERT] severity={} kind={} userId={} description={} status={}",
                command.severity(),
                command.kind(),
                command.userId(),
                command.description(),
                command.status());

        sendPush(alert);
    }

    private void touchExisting(Alert existing, NotifyUserCommand command) {
        boolean escalated = command.severity().compareTo(existing.getSeverity()) > 0;
        if (escalated) {
            existing.escalateSeverity(command.severity());
        } else {
            existing.touch();
        }
        alertRepository.save(existing);

        log.info("[ALERT] duplicate suppressed, existing alert refreshed. id={} ruleId={} eventId={}",
                existing.getId(), command.ruleId(), command.eventId());

        // um "touch" sem mudanca de severidade nao tem conteudo novo pro usuario -- pushar aqui
        // seria so repetir a mesma notificacao de novo.
        if (escalated) {
            sendPush(existing);
        }
    }

    /**
     * Best-effort e nao bloqueante: a persistencia do alerta (acima) ja aconteceu, entao qualquer
     * falha aqui so e logada -- o alerta continua valido e visivel via GET /notifications/alerts.
     */
    private void sendPush(Alert alert) {
        try {
            List<String> tokens = deviceTokenRepository.findByUserId(alert.getUserId()).stream()
                    .map(DeviceToken::getToken)
                    .toList();
            if (tokens.isEmpty()) {
                return;
            }

            PushMessage message = new PushMessage(
                    alert.getKind().displayTitle(),
                    alert.getDescription(),
                    Map.of(
                            "alertId", alert.getId().toString(),
                            "kind", alert.getKind().name(),
                            "severity", alert.getSeverity().name()));

            pushNotificationGateway.sendToTokens(tokens, message);
        } catch (RuntimeException e) {
            log.warn("Falha ao disparar push para o alerta {}", alert.getId(), e);
        }
    }
}
