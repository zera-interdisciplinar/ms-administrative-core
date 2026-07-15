package com.zera.ms_administrative_core.core.usecase.deactivateUser;

import java.util.UUID;

public interface DeactivateUser {
    void execute(UUID userId);
}
