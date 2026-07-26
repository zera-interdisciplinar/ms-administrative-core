package com.zera.ms_administrative_core.core.usecase.recycling.renameRecycling;

import java.util.UUID;

public interface RenameRecycling {
    void execute(UUID recyclingId, String name);
}