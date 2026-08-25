package com.zera.ms_administrative_core.core.usecase.unit.renameUnit;

import java.util.UUID;

public interface RenameUnit {
    void execute(UUID id, String newName);
}
