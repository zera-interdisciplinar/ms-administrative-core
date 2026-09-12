package com.zera.ms_administrative_core.core.usecase.auth;

import java.util.UUID;

/** Resultado de um login ou refresh bem-sucedido. */
public record TokenPair(
        UUID userId,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {
    public static TokenPair bearer(UUID userId, String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenPair(userId, accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
