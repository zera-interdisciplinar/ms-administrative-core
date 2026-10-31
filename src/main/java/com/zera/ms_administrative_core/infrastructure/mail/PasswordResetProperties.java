package com.zera.ms_administrative_core.infrastructure.mail;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao do fluxo de recuperacao de senha (independente do provedor de e-mail escolhido).
 *
 * <p>{@code deepLinkBase} nao e uma URL http: a tela de redefinicao e so no app mobile, entao o
 * e-mail carrega um deep link de esquema customizado (ex.: {@code zera://reset-password}) que o SO
 * abre direto no app instalado. Sem fallback web de proposito -- ver decisao de produto.
 */
@ConfigurationProperties(prefix = "zera.mail.reset")
public record PasswordResetProperties(
        String deepLinkBase,
        Duration tokenTtl
) {
    public PasswordResetProperties {
        if (deepLinkBase == null || deepLinkBase.isBlank()) {
            deepLinkBase = "zera://reset-password";
        }
        if (tokenTtl == null) {
            tokenTtl = Duration.ofMinutes(30);
        }
    }
}
