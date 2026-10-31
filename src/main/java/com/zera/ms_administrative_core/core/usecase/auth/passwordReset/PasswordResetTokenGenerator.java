package com.zera.ms_administrative_core.core.usecase.auth.passwordReset;

import java.time.Duration;

/**
 * Porta para geracao, hashing e tempo de vida do token opaco de recuperacao de senha. Mesmo
 * esquema do RefreshTokenGenerator, mas com TTL proprio (bem mais curto) e sem revogacao em massa.
 */
public interface PasswordResetTokenGenerator {

    /** Gera um novo valor bruto (vai no link enviado por e-mail, nunca persistido). */
    String newRawToken();

    /** Deriva o hash persistivel de um valor bruto. */
    String hash(String rawToken);

    /** Janela de validade de um token recem-emitido. */
    Duration timeToLive();
}
