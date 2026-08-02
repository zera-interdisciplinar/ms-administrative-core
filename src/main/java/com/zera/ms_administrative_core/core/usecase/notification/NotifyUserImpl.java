package com.zera.ms_administrative_core.core.usecase.notification;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        Alert alert = new Alert(command.kind(), command.severity(), command.description(), command.userId(),
                command.ruleId(), null, command.eventId(), command.status(), UUID.randomUUID());

        alertRepository.save(alert);

        log.info("[ALERT] severity={} kind={} userId={} description={} status={}",
                command.severity(),
                command.kind(),
                command.userId(),
                command.description(),
                command.status());

    }
}
