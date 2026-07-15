package com.zera.ms_administrative_core.core.usecase.rename;

import java.util.UUID;

public interface RenameUser {
    void execute(UUID userId, String newName);
}
