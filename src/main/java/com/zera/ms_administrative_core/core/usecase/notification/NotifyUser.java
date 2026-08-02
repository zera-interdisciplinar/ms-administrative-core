package com.zera.ms_administrative_core.core.usecase.notification;

public interface NotifyUser {
    void execute(NotifyUserCommand command);
    // TODO criar use case para editar status e severidade de alert
}
