package com.zera.ms_administrative_core.core.usecase.user.activateUser;

import java.util.UUID;

public interface ActivateUser {
    void execute(UUID userId);
}
