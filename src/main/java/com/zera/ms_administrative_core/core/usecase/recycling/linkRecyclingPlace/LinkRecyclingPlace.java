package com.zera.ms_administrative_core.core.usecase.recycling.linkRecyclingPlace;

import java.util.UUID;

public interface LinkRecyclingPlace {
    void execute(UUID recyclingBusinessId, String placeId);
}
