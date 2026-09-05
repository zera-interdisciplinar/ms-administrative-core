package com.zera.ms_administrative_core.core.usecase.auth;

import java.time.Duration;

/**
 * Porta para geracao, hashing e tempo de vida dos refresh tokens opacos. Implementada na
 * infraestrutura (SecureRandom + SHA-256).
 */
public interface RefreshTokenGenerator {

    /** Gera um novo valor bruto (entregue ao cliente uma unica vez). */
    String newRawToken();

    /** Deriva o hash persistivel de um valor bruto. */
    String hash(String rawToken);

    /** Janela de validade de um refresh token recem-emitido. */
    Duration timeToLive();
}
