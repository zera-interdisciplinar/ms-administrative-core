package com.zera.ms_administrative_core.infrastructure.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao de emissao/validacao de JWT.
 *
 * <p>{@code privateKey}/{@code publicKey} sao PEMs (PKCS#8 para a privada, X.509/SPKI para a
 * publica). Quando ambas ficam em branco, {@link RsaKeyProvider} gera um par efemero apenas para
 * ambientes de desenvolvimento.
 */
@ConfigurationProperties(prefix = "zera.jwt")
public record JwtProperties(
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        String privateKey,
        String publicKey
) {
    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "ms-administrative-core";
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(7);
        }
    }
}
