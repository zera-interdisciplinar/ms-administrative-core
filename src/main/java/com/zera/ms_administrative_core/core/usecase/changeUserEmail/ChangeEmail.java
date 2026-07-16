package com.zera.ms_administrative_core.core.usecase.changeUserEmail;

import java.util.UUID;

public interface ChangeEmail {
    void execute(UUID userId, String newEmail);
}
