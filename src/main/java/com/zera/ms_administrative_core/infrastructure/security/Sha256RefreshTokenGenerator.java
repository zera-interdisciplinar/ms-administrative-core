package com.zera.ms_administrative_core.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.stereotype.Component;

import com.zera.ms_administrative_core.core.usecase.auth.RefreshTokenGenerator;

/**
 * Refresh token = 256 bits aleatorios em base64url; persistimos apenas o SHA-256 (64 chars hex).
 * SHA-256 e suficiente aqui: o valor bruto ja tem entropia alta, ao contrario de uma senha.
 */
@Component
public class Sha256RefreshTokenGenerator implements RefreshTokenGenerator {

    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration timeToLive;

    public Sha256RefreshTokenGenerator(JwtProperties properties) {
        this.timeToLive = properties.refreshTokenTtl();
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
