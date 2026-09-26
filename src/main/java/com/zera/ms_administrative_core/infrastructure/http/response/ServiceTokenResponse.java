package com.zera.ms_administrative_core.infrastructure.http.response;

import com.zera.ms_administrative_core.core.usecase.auth.ServiceToken;

public record ServiceTokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static ServiceTokenResponse from(ServiceToken token) {
        return new ServiceTokenResponse(token.accessToken(), token.tokenType(), token.expiresInSeconds());
    }
}
