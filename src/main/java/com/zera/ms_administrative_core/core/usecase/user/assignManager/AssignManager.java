package com.zera.ms_administrative_core.core.usecase.user.assignManager;

import java.util.UUID;

public interface AssignManager {
    void execute(UUID userId, UUID managerId);
}
