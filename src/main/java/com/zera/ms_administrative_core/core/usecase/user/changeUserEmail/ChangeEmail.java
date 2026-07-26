package com.zera.ms_administrative_core.core.usecase.user.changeUserEmail;

import java.util.UUID;

public interface ChangeEmail {
    void execute(UUID userId, String newEmail);
}
