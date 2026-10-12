package com.zera.ms_administrative_core.infrastructure.push.fcm;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.zera.ms_administrative_core.core.repository.PushNotificationGateway;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O caso "ligado com credencial valida" exigiria uma service account real (chave RSA real) pra
 * {@code GoogleCredentials.fromStream} nao falhar no parse -- fica coberto pelos testes unitarios
 * de {@link FcmPushNotificationGatewayTest}, que mockam o {@code FirebaseMessaging} direto.
 */
class FcmWiringTest {

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @DisplayName("When FCM is disabled (the default)")
    class Disabled {

        @Autowired
        private PushNotificationGateway pushNotificationGateway;

        @Test
        @DisplayName("Should register the no-op gateway, never the real FCM client")
        void shouldRegisterNoopGateway() {
            assertThat(pushNotificationGateway).isInstanceOf(NoopPushNotificationGateway.class);
        }
    }

    @Nested
    @SpringBootTest
    @ActiveProfiles("test")
    @TestPropertySource(properties = "zera.fcm.enabled=true")
    @DisplayName("When FCM is enabled but the credential secret was not applied yet")
    class EnabledWithoutCredential {

        @Autowired
        private PushNotificationGateway pushNotificationGateway;

        @Test
        @DisplayName("Should fall back to the no-op gateway instead of crash-looping the boot")
        void shouldFallBackToNoopInsteadOfCrashing() {
            assertThat(pushNotificationGateway).isInstanceOf(NoopPushNotificationGateway.class);
        }
    }
}
