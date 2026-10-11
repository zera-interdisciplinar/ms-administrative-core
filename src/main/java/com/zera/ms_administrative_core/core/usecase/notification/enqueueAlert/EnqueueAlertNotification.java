package com.zera.ms_administrative_core.core.usecase.notification.enqueueAlert;

import com.zera.ms_administrative_core.core.usecase.notification.NotifyUserCommand;

public interface EnqueueAlertNotification {
    void execute(NotifyUserCommand command);
}
