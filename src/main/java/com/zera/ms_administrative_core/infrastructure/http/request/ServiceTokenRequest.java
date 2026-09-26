package com.zera.ms_administrative_core.infrastructure.http.request;

import jakarta.validation.constraints.NotBlank;

/** Credenciais do cliente de serviço; o segredo vem do secret do ambiente, nunca de usuário. */
public record ServiceTokenRequest(@NotBlank String clientId, @NotBlank String clientSecret) {
}
