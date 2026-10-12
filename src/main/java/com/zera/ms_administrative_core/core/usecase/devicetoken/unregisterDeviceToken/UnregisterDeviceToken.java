package com.zera.ms_administrative_core.core.usecase.devicetoken.unregisterDeviceToken;

import java.util.UUID;

public interface UnregisterDeviceToken {
    void execute(UUID userId, String token);
}
