package com.zera.ms_administrative_core.infrastructure.push.fcm;

import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.firebase.FirebaseApp;
import com.zera.ms_administrative_core.core.repository.DeviceTokenRepository;
import com.zera.ms_administrative_core.core.repository.PushNotificationGateway;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chave RSA gerada em memoria, de proposito: {@code GoogleCredentials.fromStream} so exige um
 * PKCS8 sintaticamente valido para montar o {@code FirebaseApp} -- nunca faz chamada de rede
 * aqui, so na hora de pedir um token de fato, que estes testes nao exercitam.
 */
class FcmConfigTest {

    private final FcmConfig config = new FcmConfig();

    @AfterEach
    void tearDown() {
        FirebaseApp.getApps().forEach(FirebaseApp::delete);
    }

    @Test
    @DisplayName("Deve montar o gateway real do FCM a partir de uma credencial valida")
    void shouldBuildRealGatewayFromValidCredential() throws IOException, NoSuchAlgorithmException {
        DeviceTokenRepository deviceTokenRepository = org.mockito.Mockito.mock(DeviceTokenRepository.class);

        PushNotificationGateway gateway = config.fcmPushNotificationGateway(
                serviceAccountJson(), deviceTokenRepository);

        assertThat(gateway).isInstanceOf(FcmPushNotificationGateway.class);
    }

    @Test
    @DisplayName("Deve reaproveitar o FirebaseApp ja inicializado em vez de falhar na segunda chamada")
    void shouldReuseAlreadyInitializedFirebaseApp() throws IOException, NoSuchAlgorithmException {
        DeviceTokenRepository deviceTokenRepository = org.mockito.Mockito.mock(DeviceTokenRepository.class);
        String credential = serviceAccountJson();

        config.fcmPushNotificationGateway(credential, deviceTokenRepository);
        // segunda chamada com o app ja inicializado: sem o cache em FcmConfig.firebaseApp,
        // FirebaseApp.initializeApp lancaria IllegalStateException aqui.
        PushNotificationGateway second = config.fcmPushNotificationGateway(credential, deviceTokenRepository);

        assertThat(second).isInstanceOf(FcmPushNotificationGateway.class);
        assertThat(FirebaseApp.getApps()).hasSize(1);
    }

    private String serviceAccountJson() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        String base64Der = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        StringBuilder pem = new StringBuilder("-----BEGIN PRIVATE KEY-----\n");
        for (int i = 0; i < base64Der.length(); i += 64) {
            pem.append(base64Der, i, Math.min(i + 64, base64Der.length())).append('\n');
        }
        pem.append("-----END PRIVATE KEY-----\n");
        String escapedPem = pem.toString().replace("\n", "\\n");

        return """
                {
                  "type": "service_account",
                  "project_id": "test-project",
                  "private_key_id": "test-key-id",
                  "private_key": "%s",
                  "client_email": "test@test-project.iam.gserviceaccount.com",
                  "client_id": "123456789",
                  "token_uri": "https://oauth2.googleapis.com/token"
                }
                """.formatted(escapedPem);
    }
}
