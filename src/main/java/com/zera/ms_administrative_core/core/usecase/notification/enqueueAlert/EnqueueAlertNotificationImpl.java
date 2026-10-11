package com.zera.ms_administrative_core.core.usecase.notification.enqueueAlert;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.repository.AlertQueueRepository;
import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

@Service
public class EnqueueAlertNotificationImpl implements EnqueueAlertNotification {

    private final AlertQueueRepository alertQueueRepository;

    public EnqueueAlertNotificationImpl(AlertQueueRepository alertQueueRepository) {
        this.alertQueueRepository = alertQueueRepository;
    }

    @Override
    public void execute(NotifyUserCommand command) {
        alertQueueRepository.enqueue(command);
    }
}
