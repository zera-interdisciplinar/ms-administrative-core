package com.zera.ms_administrative_core.core.usecase.devicetoken.registerDeviceToken;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.entity.DeviceToken;
import com.zera.ms_administrative_core.core.domain.valueobject.DevicePlatform;
import com.zera.ms_administrative_core.core.repository.DeviceTokenRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterDeviceTokenImplTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private RegisterDeviceTokenImpl useCase;

    @Test
    @DisplayName("Deve fazer upsert do device token recebido")
    void shouldUpsertDeviceToken() {
        useCase = new RegisterDeviceTokenImpl(deviceTokenRepository);
        UUID userId = UUID.randomUUID();
        when(deviceTokenRepository.upsert(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        useCase.execute(userId, "token-abc", DevicePlatform.ANDROID);

        ArgumentCaptor<DeviceToken> captor = ArgumentCaptor.forClass(DeviceToken.class);
        verify(deviceTokenRepository).upsert(captor.capture());

        DeviceToken saved = captor.getValue();
        assertEquals(userId, saved.getUserId());
        assertEquals("token-abc", saved.getToken());
        assertEquals(DevicePlatform.ANDROID, saved.getPlatform());
    }
}
