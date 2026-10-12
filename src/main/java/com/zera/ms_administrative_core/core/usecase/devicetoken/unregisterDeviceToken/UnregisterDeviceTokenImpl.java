package com.zera.ms_administrative_core.core.usecase.devicetoken.unregisterDeviceToken;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.zera.ms_administrative_core.core.repository.DeviceTokenRepository;

@Service
public class UnregisterDeviceTokenImpl implements UnregisterDeviceToken {

    private final DeviceTokenRepository deviceTokenRepository;

    public UnregisterDeviceTokenImpl(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Override
    public void execute(UUID userId, String token) {
        // so remove se o token for do proprio usuario -- sem isso, quem souber um token de outro
        // device conseguiria desregistrar o push de outra pessoa.
        boolean belongsToUser = deviceTokenRepository.findByUserId(userId).stream()
                .anyMatch(deviceToken -> deviceToken.getToken().equals(token));
        if (belongsToUser) {
            deviceTokenRepository.deleteByToken(token);
        }
    }
}
