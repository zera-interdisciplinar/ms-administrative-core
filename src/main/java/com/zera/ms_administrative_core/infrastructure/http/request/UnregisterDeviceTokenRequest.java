package com.zera.ms_administrative_core.infrastructure.http.request;

import jakarta.validation.constraints.NotBlank;

public record UnregisterDeviceTokenRequest(@NotBlank String token) {
}
