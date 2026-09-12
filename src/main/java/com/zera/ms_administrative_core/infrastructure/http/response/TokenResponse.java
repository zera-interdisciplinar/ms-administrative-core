package com.zera.ms_administrative_core.infrastructure.http.response;

import java.util.UUID;

import com.zera.ms_administrative_core.core.usecase.auth.TokenPair;

public record TokenResponse(
        UUID userId,
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse from(TokenPair pair) {
        return new TokenResponse(
                pair.userId(),
                pair.accessToken(),
                pair.refreshToken(),
                pair.tokenType(),
                pair.expiresInSeconds());
    }
}
