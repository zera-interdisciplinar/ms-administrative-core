package com.zera.ms_administrative_core.infrastructure.push.fcm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.zera.ms_administrative_core.core.repository.DeviceTokenRepository;
import com.zera.ms_administrative_core.core.repository.PushNotificationGateway;

@Configuration
public class FcmConfig {

    private static final Logger log = LoggerFactory.getLogger(FcmConfig.class);

    @Bean
    @ConditionalOnProperty(name = "zera.fcm.enabled", havingValue = "true")
    PushNotificationGateway fcmPushNotificationGateway(
            @Value("${zera.fcm.credentials-json:}") String credentialsJson,
            DeviceTokenRepository deviceTokenRepository) throws IOException {

        // zera.fcm.enabled=true sem o secret ainda aplicado (ordem de deploy, ou credencial
        // esquecida) nao pode derrubar o boot inteiro -- mesmo espirito do Google Places, que
        // sobe com a integracao desligada em vez de crash loop.
        if (credentialsJson == null || credentialsJson.isBlank()) {
            log.warn("zera.fcm.enabled=true mas FCM_CREDENTIALS_JSON esta vazio; push fica desligado");
            return new NoopPushNotificationGateway();
        }

        FirebaseApp app = firebaseApp(credentialsJson);
        return new FcmPushNotificationGateway(FirebaseMessaging.getInstance(app), deviceTokenRepository);
    }

    @Bean
    @ConditionalOnProperty(name = "zera.fcm.enabled", havingValue = "false", matchIfMissing = true)
    PushNotificationGateway noopPushNotificationGateway() {
        return new NoopPushNotificationGateway();
    }

    // o app do Firebase e um singleton por credencial no processo inteiro; initializeApp de novo
    // com o mesmo nome lanca IllegalStateException, entao reaproveita se ja existir (ex: contexto
    // de teste que sobe duas vezes).
    private FirebaseApp firebaseApp(String credentialsJson) throws IOException {
        List<FirebaseApp> existing = FirebaseApp.getApps();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        GoogleCredentials credentials = GoogleCredentials.fromStream(
                new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8)));
        FirebaseOptions options = FirebaseOptions.builder().setCredentials(credentials).build();
        return FirebaseApp.initializeApp(options);
    }
}
