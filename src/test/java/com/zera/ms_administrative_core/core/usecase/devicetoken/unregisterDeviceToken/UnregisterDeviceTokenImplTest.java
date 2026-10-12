package com.zera.ms_administrative_core.core.usecase.devicetoken.unregisterDeviceToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.zera.ms_administrative_core.core.domain.entity.DeviceToken;
import com.zera.ms_administrative_core.core.domain.valueobject.DevicePlatform;
import com.zera.ms_administrative_core.core.repository.DeviceTokenRepository;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnregisterDeviceTokenImplTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private UnregisterDeviceTokenImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new UnregisterDeviceTokenImpl(deviceTokenRepository);
    }

    @Test
    @DisplayName("Deve remover o token quando ele pertence ao usuario")
    void shouldDeleteWhenTokenBelongsToUser() {
        UUID userId = UUID.randomUUID();
        DeviceToken owned = new DeviceToken(UUID.randomUUID(), userId, "token-abc", DevicePlatform.ANDROID,
                LocalDateTime.now(), LocalDateTime.now());
        when(deviceTokenRepository.findByUserId(userId)).thenReturn(List.of(owned));

        useCase.execute(userId, "token-abc");

        verify(deviceTokenRepository).deleteByToken("token-abc");
    }

    @Test
    @DisplayName("Nao deve remover um token que pertence a outro usuario")
    void shouldNotDeleteTokenFromAnotherUser() {
        UUID userId = UUID.randomUUID();
        when(deviceTokenRepository.findByUserId(userId)).thenReturn(List.of());

        useCase.execute(userId, "token-de-outro-usuario");

        verify(deviceTokenRepository, never()).deleteByToken(org.mockito.ArgumentMatchers.any());
    }
}
