package com.zera.ms_administrative_core.core.usecase.renameUser;

import java.util.UUID;

public interface RenameUser {
    void execute(UUID userId, String newName);
}
