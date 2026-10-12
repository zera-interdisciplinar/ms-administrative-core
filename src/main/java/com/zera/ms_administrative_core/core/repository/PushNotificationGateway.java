package com.zera.ms_administrative_core.core.repository;

import java.util.List;

import com.zera.ms_administrative_core.core.domain.valueobject.PushMessage;

public interface PushNotificationGateway {
    /**
     * Best-effort: implementacoes nunca devem propagar excecao daqui. Um push que falha nunca
     * pode derrubar o fluxo de alerta que o chamou.
     */
    void sendToTokens(List<String> tokens, PushMessage message);
}
