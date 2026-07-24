package com.zera.ms_administrative_core.core.usecase.user.suspendUser;

import java.util.UUID;

public interface SuspendUser {
    void execute(UUID userId);
}
