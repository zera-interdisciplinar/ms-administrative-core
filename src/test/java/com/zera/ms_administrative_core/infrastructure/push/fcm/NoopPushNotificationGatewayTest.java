package com.zera.ms_administrative_core.infrastructure.push.fcm;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zera.ms_administrative_core.core.domain.valueobject.PushMessage;

import static org.assertj.core.api.Assertions.assertThatCode;

class NoopPushNotificationGatewayTest {

    private final NoopPushNotificationGateway gateway = new NoopPushNotificationGateway();

    @Test
    @DisplayName("Nao deve fazer nada nem lancar excecao quando chamado")
    void shouldDoNothingSilently() {
        assertThatCode(() -> gateway.sendToTokens(
                List.of("token-abc"), new PushMessage("titulo", "corpo", Map.of())))
                .doesNotThrowAnyException();
    }
}
