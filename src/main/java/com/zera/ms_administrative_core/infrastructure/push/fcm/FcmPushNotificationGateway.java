package com.zera.ms_administrative_core.infrastructure.push.fcm;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.zera.ms_administrative_core.core.domain.valueobject.PushMessage;
import com.zera.ms_administrative_core.core.repository.DeviceTokenRepository;
import com.zera.ms_administrative_core.core.repository.PushNotificationGateway;

/**
 * Best-effort de proposito: um push que falha nao pode derrubar o fluxo de alerta que o chamou
 * (ver NotifyUserImpl). Por isso nenhuma excecao escapa de {@link #sendToTokens}, so log.
 */
class FcmPushNotificationGateway implements PushNotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationGateway.class);

    private final FirebaseMessaging firebaseMessaging;
    private final DeviceTokenRepository deviceTokenRepository;

    FcmPushNotificationGateway(FirebaseMessaging firebaseMessaging, DeviceTokenRepository deviceTokenRepository) {
        this.firebaseMessaging = firebaseMessaging;
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Override
    public void sendToTokens(List<String> tokens, PushMessage message) {
        if (tokens.isEmpty()) {
            return;
        }

        MulticastMessage multicastMessage = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(message.title())
                        .setBody(message.body())
                        .build())
                .putAllData(message.data())
                .build();

        try {
            BatchResponse response = firebaseMessaging.sendEachForMulticast(multicastMessage);
            cleanUpDeadTokens(tokens, response);
        } catch (FirebaseMessagingException | RuntimeException e) {
            // nunca propaga: o alerta ja foi persistido, o push e so um canal extra de entrega.
            log.warn("Falha ao enviar push para {} token(s), titulo={}", tokens.size(), message.title(), e);
        }
    }

    /**
     * Token com UNREGISTERED/INVALID_ARGUMENT nunca mais vai funcionar (app desinstalado, token
     * expirado etc.); sem essa limpeza, cada alerta futuro tentaria de novo pro mesmo token morto.
     */
    private void cleanUpDeadTokens(List<String> tokens, BatchResponse response) {
        List<SendResponse> responses = response.getResponses();
        for (int i = 0; i < responses.size(); i++) {
            SendResponse sendResponse = responses.get(i);
            if (sendResponse.isSuccessful()) {
                continue;
            }
            FirebaseMessagingException exception = sendResponse.getException();
            MessagingErrorCode errorCode = exception != null ? exception.getMessagingErrorCode() : null;
            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                deviceTokenRepository.deleteByToken(tokens.get(i));
            }
        }
    }
}
