package com.zera.ms_administrative_core.infrastructure.push.fcm;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zera.ms_administrative_core.core.domain.valueobject.PushMessage;
import com.zera.ms_administrative_core.core.repository.PushNotificationGateway;

/** Adaptador padrao quando o FCM esta desligado (zera.fcm.enabled=false, o padrao). */
class NoopPushNotificationGateway implements PushNotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(NoopPushNotificationGateway.class);

    @Override
    public void sendToTokens(List<String> tokens, PushMessage message) {
        log.debug("Push desligado (zera.fcm.enabled=false); {} token(s) nao notificado(s), titulo={}",
                tokens.size(), message.title());
    }
}
