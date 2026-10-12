package com.zera.ms_administrative_core.infrastructure.http.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import com.zera.ms_administrative_core.core.domain.valueobject.DevicePlatform;

public record RegisterDeviceTokenRequest(@NotBlank String token, @NotNull DevicePlatform platform) {
}
