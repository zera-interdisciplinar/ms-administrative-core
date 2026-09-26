package com.zera.ms_administrative_core.core.usecase.auth;

/** Token de servico emitido para um cliente interno. */
public record ServiceToken(String accessToken, String tokenType, long expiresInSeconds) {
}
