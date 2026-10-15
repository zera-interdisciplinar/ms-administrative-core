package com.zera.ms_administrative_core.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.usecase.auth.passwordReset.PasswordResetTokenGenerator;
import com.zera.ms_administrative_core.infrastructure.mail.PasswordResetProperties;

/**
 * Mesmo esquema do Sha256RefreshTokenGenerator (256 bits aleatorios em base64url, hash SHA-256
 * persistido), com TTL proprio e bem mais curto: o token so precisa viver o tempo de o usuario
 * clicar no link do e-mail.
 */
@Component
public class Sha256PasswordResetTokenGenerator implements PasswordResetTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration timeToLive;

    public Sha256PasswordResetTokenGenerator(PasswordResetProperties properties) {
        this.timeToLive = properties.tokenTtl();
    }

    @Override
    public String newRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponivel na JVM", e);
        }
    }

    @Override
    public Duration timeToLive() {
        return timeToLive;
    }
}
