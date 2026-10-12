package com.zera.ms_administrative_core.infrastructure.push.fcm;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.zera.ms_administrative_core.core.domain.valueobject.PushMessage;
import com.zera.ms_administrative_core.core.repository.DeviceTokenRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmPushNotificationGatewayTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    private FcmPushNotificationGateway gateway;

    @Test
    @DisplayName("Nao deve chamar o FCM quando a lista de tokens esta vazia")
    void shouldNotCallFcmWithoutTokens() throws FirebaseMessagingException {
        gateway = new FcmPushNotificationGateway(firebaseMessaging, deviceTokenRepository);

        gateway.sendToTokens(List.of(), new PushMessage("titulo", "corpo", Map.of()));

        verify(firebaseMessaging, never()).sendEachForMulticast(any(MulticastMessage.class));
    }

    @Test
    @DisplayName("Deve limpar token morto quando o FCM devolve UNREGISTERED")
    void shouldDeleteDeadToken() throws FirebaseMessagingException {
        gateway = new FcmPushNotificationGateway(firebaseMessaging, deviceTokenRepository);

        SendResponse deadResponse = org.mockito.Mockito.mock(SendResponse.class);
        when(deadResponse.isSuccessful()).thenReturn(false);
        FirebaseMessagingException exception = org.mockito.Mockito.mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(deadResponse.getException()).thenReturn(exception);

        SendResponse okResponse = org.mockito.Mockito.mock(SendResponse.class);
        when(okResponse.isSuccessful()).thenReturn(true);

        BatchResponse batchResponse = org.mockito.Mockito.mock(BatchResponse.class);
        when(batchResponse.getResponses()).thenReturn(List.of(deadResponse, okResponse));
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

        gateway.sendToTokens(List.of("token-morto", "token-vivo"), new PushMessage("titulo", "corpo", Map.of()));

        verify(deviceTokenRepository).deleteByToken("token-morto");
        verify(deviceTokenRepository, never()).deleteByToken("token-vivo");
    }

    @Test
    @DisplayName("Nao deve propagar excecao quando o FCM falha")
    void shouldNeverPropagateFcmFailure() throws FirebaseMessagingException {
        gateway = new FcmPushNotificationGateway(firebaseMessaging, deviceTokenRepository);
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class)))
                .thenThrow(org.mockito.Mockito.mock(FirebaseMessagingException.class));

        gateway.sendToTokens(List.of("token-abc"), new PushMessage("titulo", "corpo", Map.of()));

        verify(deviceTokenRepository, never()).deleteByToken(any());
    }
}
