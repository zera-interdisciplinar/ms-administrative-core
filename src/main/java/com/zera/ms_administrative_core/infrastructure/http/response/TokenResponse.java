package com.zera.ms_administrative_core.infrastructure.http.response;

import com.zera.ms_administrative_core.core.usecase.auth.TokenPair;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {
    public static TokenResponse from(TokenPair pair) {
        return new TokenResponse(
                pair.accessToken(),
                pair.refreshToken(),
                pair.tokenType(),
                pair.expiresInSeconds());
    }
}
