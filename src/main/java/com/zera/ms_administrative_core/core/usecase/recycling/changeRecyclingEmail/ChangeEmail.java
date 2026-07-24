package com.zera.ms_administrative_core.core.usecase.recycling.changeRecyclingEmail;

import java.util.UUID;

public interface ChangeEmail {
    void execute(UUID recyclingId, String email);
}
