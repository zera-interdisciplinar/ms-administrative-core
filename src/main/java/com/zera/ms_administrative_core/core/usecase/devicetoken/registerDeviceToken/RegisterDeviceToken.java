package com.zera.ms_administrative_core.core.usecase.devicetoken.registerDeviceToken;

import java.util.UUID;

import com.zera.ms_administrative_core.core.domain.valueobject.DevicePlatform;

public interface RegisterDeviceToken {
    void execute(UUID userId, String token, DevicePlatform platform);
}
