package com.zera.ms_administrative_core.core.usecase.auth.passwordReset;

/**
 * Porta que monta a URL do link de recuperacao a partir do token bruto. A URL base do frontend e
 * configuracao de ambiente, por isso fica na infraestrutura -- o dominio so sabe que existe um
 * link.
 */
public interface PasswordResetLinkFactory {
    String buildLink(String rawToken);
}
