package com.zera.ms_administrative_core.core.usecase.auth;

/** Resultado de um login ou refresh bem-sucedido. */
public record TokenPair(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds
) {
    public static TokenPair bearer(String accessToken, String refreshToken, long expiresInSeconds) {
        return new TokenPair(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }
}
