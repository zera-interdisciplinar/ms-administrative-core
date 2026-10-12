package com.zera.ms_administrative_core.core.usecase.devicetoken.registerDeviceToken;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.domain.entity.DeviceToken;
import com.zera.ms_administrative_core.core.domain.valueobject.DevicePlatform;
import com.zera.ms_administrative_core.core.repository.DeviceTokenRepository;

@Service
public class RegisterDeviceTokenImpl implements RegisterDeviceToken {

    private final DeviceTokenRepository deviceTokenRepository;

    public RegisterDeviceTokenImpl(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Override
    public void execute(UUID userId, String token, DevicePlatform platform) {
        LocalDateTime now = LocalDateTime.now();
        deviceTokenRepository.upsert(new DeviceToken(UUID.randomUUID(), userId, token, platform, now, now));
    }
}
