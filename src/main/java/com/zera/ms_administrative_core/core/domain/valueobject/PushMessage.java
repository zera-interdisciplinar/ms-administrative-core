package com.zera.ms_administrative_core.core.domain.valueobject;

import java.util.Map;

/** {@code data} carrega os campos que o app usa pra montar o deep link ao abrir a notificacao. */
public record PushMessage(String title, String body, Map<String, String> data) {
}
